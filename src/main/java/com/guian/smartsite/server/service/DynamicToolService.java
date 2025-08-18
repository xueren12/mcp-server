package com.guian.smartsite.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guian.smartsite.server.config.DatabaseApiToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DynamicToolService {
    
    private final DatabaseApiToolConfig databaseApiToolConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${mcp.client.base-url:http://localhost:8080}")
    private String mcpClientBaseUrl;
    
    public DynamicToolService(DatabaseApiToolConfig databaseApiToolConfig) {
        this.databaseApiToolConfig = databaseApiToolConfig;
        this.objectMapper = new ObjectMapper();
        
        // 创建WebClient并设置超时
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }
    
    public Mono<String> callApi(String toolName, Map<String, Object> parameters) {
        DatabaseApiToolConfig.ApiTool tool = findToolByName(toolName);
        if (tool == null) {
            return Mono.just("工具不存在: " + toolName);
        }
        
        return callApiInternal(tool, parameters);
    }
    
    /**
     * 内部API调用方法（核心逻辑）
     */
    private Mono<String> callApiInternal(DatabaseApiToolConfig.ApiTool tool, Map<String, Object> parameters) {
        // 处理静态工具 - 直接返回mockData
        if ("STATIC".equalsIgnoreCase(tool.getApiType()) && tool.getMockData() != null) {
            log.info("调用静态工具: {} - 返回mockData", tool.getName());
            return Mono.just(addTypeToResponse(tool.getMockData(), tool.getDataType()));
        }
        
        try {
            log.info("调用API工具: {} - {}", tool.getName(), tool.getUrl());
            
            String url = tool.getUrl();
             
            if ("GET".equalsIgnoreCase(tool.getMethod())) {
                // GET请求：将参数添加到URL
                if (parameters != null && !parameters.isEmpty()) {
                    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
                    parameters.forEach((key, value) -> {
                        if (value instanceof List) {
                            // 处理List类型参数，将每个元素作为单独的查询参数
                            List<?> listValue = (List<?>) value;
                            for (Object item : listValue) {
                                builder.queryParam(key, item);
                            }
                        } else {
                            builder.queryParam(key, value);
                        }
                    });
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
                        .map(response -> addTypeToResponse(response, tool.getDataType()))
                        .doOnSuccess(response -> log.info("API工具 {} 调用成功", tool.getName()))
                        .onErrorResume(WebClientResponseException.class, e -> {
                            log.error("API工具 {} 调用失败，状态码: {}", tool.getName(), e.getStatusCode(), e);
                            return Mono.just("调用API工具失败: " + e.getMessage());
                        })
                        .onErrorResume(Exception.class, e -> {
                            log.error("调用API工具 {} 时发生异常", tool.getName(), e);
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
                
                return requestSpec
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .map(response -> addTypeToResponse(response, tool.getDataType()))
                        .doOnSuccess(response -> log.info("API工具 {} 调用成功", tool.getName()))
                        .onErrorResume(WebClientResponseException.class, e -> {
                            log.error("API工具 {} 调用失败，状态码: {}", tool.getName(), e.getStatusCode(), e);
                            return Mono.just("调用API工具失败: " + e.getMessage());
                        })
                        .onErrorResume(Exception.class, e -> {
                            log.error("调用API工具 {} 时发生异常", tool.getName(), e);
                            return Mono.just("调用API工具失败: " + e.getMessage());
                        });
            }
            
        } catch (Exception e) {
            log.error("调用API工具 {} 时发生异常", tool.getName(), e);
            return Mono.just("调用API工具失败: " + e.getMessage());
        }
    }
    
    /**
     * 为MCP协议调用API工具
     */
    public Mono<String> callApiWithType(String toolName, Map<String, Object> parameters) {
        log.info("调用API工具: {}", toolName);
        
        // 直接调用API，
        return callApi(toolName, parameters)
            .doOnSuccess(response -> log.info("API调用成功，响应长度: {}", response.length()))
            .doOnError(error -> log.error("API调用失败", error));
    }
    
    /**
     * 为MCP协议调用API工具（带项目ID验证）
     */
    public Mono<String> callApiWithTypeAndProjectId(String toolName, Map<String, Object> parameters, Long projectId) {
        log.info("调用API工具: {}，项目ID: {}", toolName, projectId);
        
        // 使用带项目ID验证的工具查找
        DatabaseApiToolConfig.ApiTool tool = findToolByNameAndProjectId(toolName, projectId);
        if (tool == null) {
            if (projectId != null) {
                return Mono.just("工具不存在或不属于指定项目: " + toolName);
            } else {
                return Mono.just("工具不存在: " + toolName);
            }
        }
        
        // 验证必需参数
        String parameterValidationError = validateRequiredParameters(tool, parameters);
        if (parameterValidationError != null) {
            log.error("参数验证失败: {}", parameterValidationError);
            return Mono.just(parameterValidationError);
        }
        
        // 直接调用API（这里重用现有的API调用逻辑）
        return callApiInternal(tool, parameters)
            .doOnSuccess(response -> log.info("API调用成功，响应长度: {}", response.length()))
            .doOnError(error -> log.error("API调用失败", error));
    }
    
    /**
     * 验证必需参数
     */
    private String validateRequiredParameters(DatabaseApiToolConfig.ApiTool tool, Map<String, Object> parameters) {
        if (tool.getParameters() == null || tool.getParameters().isEmpty()) {
            log.debug("工具 {} 没有定义参数", tool.getName());
            return null;
        }
        
        log.debug("开始验证工具 {} 的必需参数", tool.getName());
        
        for (Map.Entry<String, DatabaseApiToolConfig.ParameterInfo> entry : tool.getParameters().entrySet()) {
            String paramName = entry.getKey();
            DatabaseApiToolConfig.ParameterInfo paramInfo = entry.getValue();
            
            if (paramInfo.isRequired()) {
                boolean hasParam = parameters != null && parameters.containsKey(paramName);
                Object paramValue = parameters != null ? parameters.get(paramName) : null;
                boolean hasValue = paramValue != null && !paramValue.toString().trim().isEmpty();
                
                log.debug("检查必需参数 {}: 存在={}, 值={}, 有效值={}", 
                    paramName, hasParam, paramValue, hasValue);
                
                if (!hasParam || !hasValue) {
                    // 生成简洁的参数提示
                    StringBuilder hint = new StringBuilder();
                    hint.append("缺少必需参数: ");
                    
                    List<String> requiredParams = new ArrayList<>();
                    for (Map.Entry<String, DatabaseApiToolConfig.ParameterInfo> paramEntry : tool.getParameters().entrySet()) {
                        DatabaseApiToolConfig.ParameterInfo info = paramEntry.getValue();
                        if (info.isRequired()) {
                            requiredParams.add(paramEntry.getKey());
                        }
                    }
                    
                    hint.append(String.join(", ", requiredParams));
                    
                    return hint.toString();
                }
            }
        }
        
        log.debug("工具 {} 的参数验证通过", tool.getName());
        return null;
    }
    
    /**
     * 根据名称查找工具
     */
    private DatabaseApiToolConfig.ApiTool findToolByName(String name) {
        return databaseApiToolConfig.getTools().stream()
            .filter(tool -> tool.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 根据名称和项目ID查找工具（带安全验证）
     */
    private DatabaseApiToolConfig.ApiTool findToolByNameAndProjectId(String name, Long projectId) {
        DatabaseApiToolConfig.ApiTool tool = findToolByName(name);
        if (tool == null) {
            return null;
        }
        
        // 如果指定了项目ID，验证工具是否属于该项目
        if (projectId != null && !projectId.equals(tool.getProjectId())) {
            log.warn("工具 {} 不属于项目 {}，实际属于项目 {}", name, projectId, tool.getProjectId());
            return null;
        }
        
        return tool;
    }

    /**
     * 向响应JSON中添加type字段
     */
    private String addTypeToResponse(String response, String dataType) {
        try {
            if (dataType == null || dataType.trim().isEmpty()) {
                return response;
            }
            
            if (response == null || response.trim().isEmpty()) {
                return response;
            }
            
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 如果是ObjectNode，直接添加type字段
            if (jsonNode instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                objectNode.put("type", dataType);
                return objectMapper.writeValueAsString(objectNode);
            }
            
            // 如果不是对象格式，包装成对象并添加type字段
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.put("type", dataType);
            wrapper.set("data", jsonNode);
            return objectMapper.writeValueAsString(wrapper);
            
        } catch (Exception e) {
            log.error("添加type字段失败", e);
            return response;
        }
    }
}