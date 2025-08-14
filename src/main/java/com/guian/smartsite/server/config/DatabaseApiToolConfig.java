package com.guian.smartsite.server.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guian.smartsite.server.entity.ApiInfo;
import com.guian.smartsite.server.repository.ApiInfoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据库API工具配置
 */
@Data
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseApiToolConfig {
    
    private final ApiInfoRepository apiInfoRepository;
    
    @Value("${api.base-url:http://116.63.146.89:60011}")
    private String apiBaseUrl;
    
    private List<ApiTool> tools;
    
    @PostConstruct
    public void loadToolsFromDatabase() {
        log.info("开始从数据库加载API配置并注册为MCP工具...");
        try {
            // 查询所有可用的API（不限制mcp_flag）
            List<ApiInfo> apiInfoList = apiInfoRepository.findByProjectId(1953288277076803585L);
            log.info("从数据库查询到 {} 条可用的API记录", apiInfoList.size());
            
            if (!apiInfoList.isEmpty()) {
                // 转换为工具配置
                this.tools = apiInfoList.stream()
                        .map(this::convertToApiTool)
                        .collect(Collectors.toList());
                
                // 批量更新这些API的mcp_flag为0（标记为已注册MCP工具）
                List<Long> apiIds = apiInfoList.stream()
                        .map(ApiInfo::getApiId)
                        .collect(Collectors.toList());
                
                int updatedCount = apiInfoRepository.updateMcpFlagToRegistered(apiIds);
                log.info("成功将 {} 个API标记为已注册MCP工具（mcp_flag=0）", updatedCount);
                
                log.info("成功从数据库加载并注册了 {} 个MCP工具", tools.size());
                
                // 输出所有工具的完整URL用于验证
                tools.forEach(tool -> 
                    log.info("MCP工具 {} 的完整URL: {}", tool.getName(), tool.getUrl())
                );
            } else {
                this.tools = List.of();
                log.warn("数据库中没有找到可用的API记录");
            }
        } catch (Exception e) {
            log.error("从数据库加载API配置并注册为MCP工具失败", e);
            this.tools = List.of(); // 设置为空列表，避免空指针异常
        }
    }
    

    
    /**
     * 将ApiInfo转换为ApiTool
     */
    private ApiTool convertToApiTool(ApiInfo apiInfo) {
        ApiTool apiTool = new ApiTool();
        apiTool.setName(apiInfo.getApiName());
        apiTool.setDescription(apiInfo.getApiDesc());
        apiTool.setApiType(apiInfo.getApiType());
        apiTool.setMockData(apiInfo.getMockData());
        apiTool.setDataType(apiInfo.getDataType());
        apiTool.setProjectId(apiInfo.getProjectId());

        String apiPath = apiInfo.getApiPath();
        String fullUrl = apiBaseUrl + "/" +apiPath;
        apiTool.setUrl(fullUrl);
        
        apiTool.setMethod(apiInfo.getApiMethod() != null ? apiInfo.getApiMethod().toUpperCase() : "GET");
        
        // 这里从requestParams字段解析参数信息
        if (apiInfo.getRequestParams() != null && !apiInfo.getRequestParams().isEmpty()) {
            try {
                // 解析JSON格式的参数定义
                Map<String, ParameterInfo> parameters = parseRequestParams(apiInfo.getRequestParams());
                apiTool.setParameters(parameters);
            } catch (Exception e) {
                log.warn("解析工具 {} 的参数定义失败: {}", apiInfo.getApiName(), e.getMessage());
                apiTool.setParameters(Map.of());
            }
        } else {
            apiTool.setParameters(Map.of());
        }
        
        return apiTool;
    }
    
    /**
      * 解析JSON格式的请求参数定义
      */
     private Map<String, ParameterInfo> parseRequestParams(String requestParamsJson) {
         try {
             ObjectMapper objectMapper = new ObjectMapper();
             Map<String, ParameterInfo> parameters = new HashMap<>();
             
             // 解析数组格式 [{"paramName": "id", "paramType": "string", "required": true, ...}]
             List<Map<String, Object>> paramsList = objectMapper.readValue(requestParamsJson, 
                 new TypeReference<List<Map<String, Object>>>() {});
             
             for (Map<String, Object> paramDef : paramsList) {
                 String paramName = (String) paramDef.get("paramName");
                 if (paramName == null || paramName.trim().isEmpty()) {
                     continue;
                 }
                 
                 ParameterInfo paramInfo = new ParameterInfo();
                 paramInfo.setType((String) paramDef.getOrDefault("paramType", "string"));
                 paramInfo.setDescription((String) paramDef.getOrDefault("paramDesc", ""));
                 
                 // 处理 required 字段，可能是 boolean 或 string
                 Object requiredObj = paramDef.get("required");
                 boolean isRequired = false;
                 if (requiredObj instanceof Boolean) {
                     isRequired = (Boolean) requiredObj;
                 } else if (requiredObj instanceof String) {
                     isRequired = "true".equalsIgnoreCase((String) requiredObj) || "1".equals(requiredObj);
                 }
                 paramInfo.setRequired(isRequired);
                 
                 // 使用 exampleValue 作为默认值
                 paramInfo.setDefaultValue(paramDef.get("exampleValue"));
                 parameters.put(paramName, paramInfo);
             }
             
             return parameters;
         } catch (Exception e) {
             log.warn("解析请求参数JSON失败: {}", e.getMessage());
             return new HashMap<>();
         }
     }
    
    @Data
    public static class ApiTool {
        private String name;
        private String description;
        private String url;
        private String method = "GET";
        private String apiType;
        private String mockData;
        private String dataType;
        private Long projectId;
        private Map<String, Object> headers;
        private Map<String, ParameterInfo> parameters;
    }
    
    @Data
    public static class ParameterInfo {
        private String type;
        private String description;
        private boolean required = false;
        private Object defaultValue;
    }
}