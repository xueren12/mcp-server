package com.guian.smartsite.server.service;

import com.guian.smartsite.server.config.DatabaseApiToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DynamicToolService {
    
    private final DatabaseApiToolConfig databaseApiToolConfig;
    private final WebClient webClient;
    
    public DynamicToolService(DatabaseApiToolConfig databaseApiToolConfig) {
        this.databaseApiToolConfig = databaseApiToolConfig;
        
        // 创建WebClient并设置超时
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }
    
    public Mono<String> callApi(String toolName, Map<String, Object> parameters) {
        DatabaseApiToolConfig.ApiTool tool = findToolByName(toolName);
        if (tool == null) {
            return Mono.just("工具不存在: " + toolName);
        }
        
        // 处理静态工具 - 直接返回mockData
        if ("STATIC".equalsIgnoreCase(tool.getApiType()) && tool.getMockData() != null) {
            log.info("调用静态工具: {} - 返回mockData", toolName);
            return Mono.just(tool.getMockData());
        }
        
        try {
            log.info("调用API工具: {} - {}", toolName, tool.getUrl());
            
            String url = tool.getUrl();
            
            if ("GET".equalsIgnoreCase(tool.getMethod())) {
                // GET请求：将参数添加到URL
                if (parameters != null && !parameters.isEmpty()) {
                    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
                    parameters.forEach((key, value) -> 
                        builder.queryParam(key, value));
                    url = builder.toUriString();
                }
                
                WebClient.RequestHeadersSpec<?> requestSpec = webClient.get().uri(url);
                
                // 设置请求头
                if (tool.getHeaders() != null) {
                    tool.getHeaders().forEach((key, value) -> 
                        requestSpec.header(key, value.toString()));
                }
                
                return requestSpec
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .doOnSuccess(response -> log.info("API工具 {} 调用成功", toolName))
                        .onErrorResume(WebClientResponseException.class, e -> {
                            log.error("API工具 {} 调用失败，状态码: {}", toolName, e.getStatusCode(), e);
                            return Mono.just("调用API工具失败: " + e.getMessage());
                        })
                        .onErrorResume(Exception.class, e -> {
                            log.error("调用API工具 {} 时发生异常", toolName, e);
                            return Mono.just("调用API工具失败: " + e.getMessage());
                        });
                        
            } else {
                // POST请求：将参数放在请求体中
                Map<String, Object> requestBody = parameters != null ? parameters : new HashMap<String, Object>();
                WebClient.RequestBodySpec requestSpec = webClient.post()
                        .uri(url);
                
                // 设置请求体
                WebClient.RequestHeadersSpec<?> requestWithBody = requestSpec.bodyValue(requestBody);
                
                // 设置请求头
                if (tool.getHeaders() != null) {
                    tool.getHeaders().forEach((key, value) -> 
                        requestWithBody.header(key, value.toString()));
                }
                
                return requestWithBody
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .doOnSuccess(response -> log.info("API工具 {} 调用成功", toolName))
                        .onErrorResume(WebClientResponseException.class, e -> {
                            log.error("API工具 {} 调用失败，状态码: {}", toolName, e.getStatusCode(), e);
                            return Mono.just("调用API工具失败: " + e.getMessage());
                        })
                        .onErrorResume(Exception.class, e -> {
                            log.error("调用API工具 {} 时发生异常", toolName, e);
                            return Mono.just("调用API工具失败: " + e.getMessage());
                        });
            }
            
        } catch (Exception e) {
            log.error("调用API工具 {} 时发生异常", toolName, e);
            return Mono.just("调用API工具失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新加载工具配置
     */
    public void reloadTools() {
        log.info("重新加载API工具配置...");
        databaseApiToolConfig.reloadTools();
        log.info("API工具配置重新加载完成");
    }
    
    private DatabaseApiToolConfig.ApiTool findToolByName(String name) {
        return databaseApiToolConfig.getTools().stream()
            .filter(tool -> tool.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}