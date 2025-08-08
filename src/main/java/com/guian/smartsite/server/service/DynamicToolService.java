package com.guian.smartsite.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guian.smartsite.server.config.DatabaseApiToolConfig;
import lombok.extern.slf4j.Slf4j;
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
        
        // 处理自然语言参数解析
        if (parameters != null && parameters.containsKey("naturalLanguageInput")) {
            String naturalLanguageInput = (String) parameters.get("naturalLanguageInput");
            log.info("检测到自然语言输入，开始解析: {}", naturalLanguageInput);
            parameters = parseNaturalLanguageParameters(naturalLanguageInput, tool);
            log.info("解析后的参数: {}", parameters);
        }
        
        // 验证必填参数
        String validationError = validateRequiredParameters(tool, parameters);
        if (validationError != null) {
            return Mono.just(validationError);
        }
        
        // 处理静态工具 - 直接返回mockData
        if ("STATIC".equalsIgnoreCase(tool.getApiType()) && tool.getMockData() != null) {
            log.info("调用静态工具: {} - 返回mockData", toolName);
            return Mono.just(addTypeToResponse(tool.getMockData(), tool.getDataType()));
        }
        
        try {
            log.info("调用API工具: {} - {}", toolName, tool.getUrl());
            
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
                        .map(response -> addTypeToResponse(response, tool.getDataType()))
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
     * 调用API工具并添加type字段 - 专门为MCP协议使用
     */
    public Mono<String> callApiWithType(String toolName, Map<String, Object> parameters) {
        log.info("=== DynamicToolService.callApiWithType 开始 ===");
        log.info("MCP协议调用API工具: {}, 参数: {}", toolName, parameters);
        
        // 查找工具信息
        DatabaseApiToolConfig.ApiTool tool = findToolByName(toolName);
        if (tool != null) {
            log.info("找到工具: {}", tool.getName());
            log.info("工具类型: {}", tool.getApiType());
            log.info("工具URL: {}", tool.getUrl());
            log.info("工具dataType: {}", tool.getDataType());
            log.info("工具mockData: {}", tool.getMockData() != null ? tool.getMockData().substring(0, Math.min(100, tool.getMockData().length())) + "..." : "null");
        } else {
            log.error("未找到工具: {}", toolName);
        }
        
        return callApi(toolName, parameters)
            .doOnNext(result -> {
                log.info("=== callApi 返回结果 ===");
                log.info("结果长度: {}", result != null ? result.length() : 0);
                log.info("结果前200字符: {}", result != null && result.length() > 200 ? result.substring(0, 200) + "..." : result);
            })
            .doOnError(error -> {
                log.error("=== callApi 发生错误 ===", error);
            });
    }

    
    private DatabaseApiToolConfig.ApiTool findToolByName(String name) {
        return databaseApiToolConfig.getTools().stream()
            .filter(tool -> tool.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 解析自然语言参数
     */
    private Map<String, Object> parseNaturalLanguageParameters(String naturalLanguageInput, DatabaseApiToolConfig.ApiTool tool) {
        Map<String, Object> parsedParams = new HashMap<>();
        
        if (naturalLanguageInput == null || naturalLanguageInput.trim().isEmpty()) {
            return parsedParams;
        }
        
        log.info("开始解析自然语言参数: {}", naturalLanguageInput);
        
        String input = naturalLanguageInput.trim();
        
        // 方法1: 使用正则表达式匹配所有 key=value 对，支持空格和逗号分隔
        String pattern = "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([^\\s,，]+(?:\\s*[^=\\s,，]*)*?)(?=\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*=|\\s*[,，]\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*=|$)";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(input);
        
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            
            // 处理数组格式 ["value1", "value2"]
            if (value.startsWith("[") && value.endsWith("]")) {
                String arrayContent = value.substring(1, value.length() - 1);
                String[] arrayValues = arrayContent.split(",");
                List<String> arrayList = new ArrayList<>();
                for (String arrayValue : arrayValues) {
                    String cleanValue = arrayValue.trim().replaceAll("^\"|\"$", "");
                    arrayList.add(cleanValue);
                }
                parsedParams.put(key, arrayList);
                log.info("解析数组参数: {} = {}", key, arrayList);
            } else {
                // 移除可能的引号和多余空格
                value = value.replaceAll("^\"|\"$", "").trim();
                parsedParams.put(key, value);
                log.info("解析字符串参数: {} = {}", key, value);
            }
        }
        
        // 方法2: 如果正则匹配失败或结果不完整，使用改进的分割方式
        if (parsedParams.isEmpty()) {
            log.warn("正则匹配失败，使用分割方式解析");
            
            // 先按逗号分割，再按空格分割
            String[] parts = input.split("[,，]");
            for (String part : parts) {
                part = part.trim();
                if (part.contains("=")) {
                    String[] keyValue = part.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        // 检查是否为数组格式
                        if (value.startsWith("[") && value.endsWith("]")) {
                            String arrayContent = value.substring(1, value.length() - 1);
                            String[] arrayValues = arrayContent.split(",");
                            List<String> arrayList = new ArrayList<>();
                            for (String arrayValue : arrayValues) {
                                String cleanValue = arrayValue.trim().replaceAll("^\"|\"$", "");
                                arrayList.add(cleanValue);
                            }
                            parsedParams.put(key, arrayList);
                            log.info("分割解析数组参数: {} = {}", key, arrayList);
                        } else {
                            parsedParams.put(key, value);
                            log.info("分割解析参数: {} = {}", key, value);
                        }
                    }
                }
            }
            
            // 如果还是没有结果，尝试按空格分割
            if (parsedParams.isEmpty()) {
                log.warn("逗号分割失败，尝试空格分割");
                parts = input.split("\\s+");
                for (String part : parts) {
                    part = part.trim();
                    if (part.contains("=")) {
                        String[] keyValue = part.split("=", 2);
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim();
                            String value = keyValue[1].trim();
                            // 移除可能的尾部逗号
                            value = value.replaceAll("[,，]$", "");
                            
                            // 检查是否为数组格式
                            if (value.startsWith("[") && value.endsWith("]")) {
                                String arrayContent = value.substring(1, value.length() - 1);
                                String[] arrayValues = arrayContent.split(",");
                                List<String> arrayList = new ArrayList<>();
                                for (String arrayValue : arrayValues) {
                                    String cleanValue = arrayValue.trim().replaceAll("^\"|\"$", "");
                                    arrayList.add(cleanValue);
                                }
                                parsedParams.put(key, arrayList);
                                log.info("空格分割解析数组参数: {} = {}", key, arrayList);
                            } else {
                                parsedParams.put(key, value);
                                log.info("空格分割解析参数: {} = {}", key, value);
                            }
                        }
                    }
                }
            }
        }
        
        log.info("参数解析完成，共解析出 {} 个参数: {}", parsedParams.size(), parsedParams);
        return parsedParams;
    }
    
    /**
     * 验证必填参数
     */
    private String validateRequiredParameters(DatabaseApiToolConfig.ApiTool tool, Map<String, Object> parameters) {
        if (tool.getParameters() == null) {
            log.info("工具 {} 没有定义参数", tool.getName());
            return null;
        }
        
        log.info("开始验证工具 {} 的必填参数", tool.getName());
        log.info("当前解析的参数: {}", parameters);
        
        for (Map.Entry<String, DatabaseApiToolConfig.ParameterInfo> entry : tool.getParameters().entrySet()) {
            String paramName = entry.getKey();
            DatabaseApiToolConfig.ParameterInfo paramInfo = entry.getValue();
            
            log.info("检查参数: {} (必需: {})", paramName, paramInfo.isRequired());
            
            if (paramInfo.isRequired()) {
                boolean hasParam = parameters != null && parameters.containsKey(paramName);
                Object paramValue = parameters != null ? parameters.get(paramName) : null;
                boolean hasValue = paramValue != null && !paramValue.toString().trim().isEmpty();
                
                log.info("参数 {} 存在: {}, 值: {}, 有效值: {}", 
                    paramName, hasParam, paramValue, hasValue);
                
                if (!hasParam || !hasValue) {
                    String errorMsg = String.format("参数%s为必填项。当前参数: %s", 
                        paramName, parameters);
                    log.error(errorMsg);
                    return errorMsg;
                }
            }
        }
        
        log.info("参数验证通过");
        return null;
    }
    
    /**
     * 向响应JSON中添加type字段
     */
    private String addTypeToResponse(String response, String dataType) {
        log.info("=== 开始添加type字段 ===");
        log.info("dataType: [{}]", dataType);
        log.info("response长度: {}", response != null ? response.length() : 0);
        log.info("response前100字符: {}", response != null && response.length() > 100 ? response.substring(0, 100) + "..." : response);
        
        try {
            if (dataType == null || dataType.trim().isEmpty()) {
                log.warn("dataType为空或空字符串，直接返回原始响应");
                return response;
            }
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("response为空，直接返回");
                return response;
            }
            
            JsonNode jsonNode = objectMapper.readTree(response);
            log.info("成功解析JSON，节点类型: {}", jsonNode.getClass().getSimpleName());
            
            // 如果是ObjectNode，直接添加type字段
            if (jsonNode instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                objectNode.put("type", dataType);
                String result = objectMapper.writeValueAsString(objectNode);
                log.info("成功添加type字段到ObjectNode");
                log.info("添加type字段后的结果长度: {}", result.length());
                log.info("添加type字段后的结果前100字符: {}", result.length() > 100 ? result.substring(0, 100) + "..." : result);
                return result;
            }
            
            // 如果不是对象格式，包装成对象并添加type字段
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.put("type", dataType);
            wrapper.set("data", jsonNode);
            String result = objectMapper.writeValueAsString(wrapper);
            log.info("成功包装并添加type字段");
            log.info("包装后的结果长度: {}", result.length());
            log.info("包装后的结果前100字符: {}", result.length() > 100 ? result.substring(0, 100) + "..." : result);
            return result;
            
        } catch (Exception e) {
            log.error("=== 添加type字段失败 ===");
            log.error("dataType: [{}]", dataType);
            log.error("response: {}", response);
            log.error("异常信息: ", e);
            return response;
        }
    }
}