package com.guian.smartsite.server.config;

import com.guian.smartsite.server.entity.ApiInfo;
import com.guian.smartsite.server.repository.ApiInfoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
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
        log.info("开始从数据库加载API工具配置...");
        try {
            List<ApiInfo> apiInfoList = apiInfoRepository.findByProjectId(1953288277076803585L);
            log.info("从数据库查询到 {} 条API记录", apiInfoList.size());
            
            this.tools = apiInfoList.stream()
                    .map(this::convertToApiTool)
                    .collect(Collectors.toList());
            log.info("成功从数据库加载了 {} 个API工具", tools.size());
            
            // 输出所有工具的完整URL用于验证
            tools.forEach(tool -> 
                log.info("工具 {} 的完整URL: {}", tool.getName(), tool.getUrl())
            );
        } catch (Exception e) {
            log.error("从数据库加载API工具配置失败", e);
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
        
        // 确保dataType有合适的值，根据工具名称智能设置
        String dataType = apiInfo.getDataType();
        if (dataType == null || dataType.trim().isEmpty()) {
            // 根据工具名称智能设置dataType
            dataType = inferDataTypeFromToolName(apiInfo.getApiName());
            log.info("工具 {} 的dataType为空，根据工具名称推断设置为: {}", apiInfo.getApiName(), dataType);
        }
        apiTool.setDataType(dataType);
        
        // 处理API路径：如果不是完整URL，则添加基础URL前缀
        String apiPath = apiInfo.getApiPath();
        String fullUrl;
        if (apiPath != null && (apiPath.startsWith("http://") || apiPath.startsWith("https://"))) {
            // 已经是完整URL，直接使用
            fullUrl = apiPath;
            log.debug("使用完整URL: {}", fullUrl);
        } else {
            // 不是完整URL，添加基础URL前缀
            if (apiPath != null && !apiPath.startsWith("/")) {
                // 如果路径不以/开头，添加/
                fullUrl = apiBaseUrl + "/" + apiPath;
            } else {
                fullUrl = apiBaseUrl + (apiPath != null ? apiPath : "");
            }
            log.debug("构建完整URL: {} + {} = {}", apiBaseUrl, apiPath, fullUrl);
        }
        apiTool.setUrl(fullUrl);
        
        apiTool.setMethod(apiInfo.getApiMethod() != null ? apiInfo.getApiMethod().toUpperCase() : "GET");
        
        // 这里可以从requestParams字段解析参数信息
        if (apiInfo.getRequestParams() != null && !apiInfo.getRequestParams().isEmpty()) {
            // 暂时设置为空，后续可以根据实际需求扩展
            apiTool.setParameters(Map.of());
        }
        
        return apiTool;
    }
    
    /**
     * 根据工具名称推断数据类型
     */
    private String inferDataTypeFromToolName(String toolName) {
        if (toolName == null) {
            return "API";
        }
        
        String name = toolName.toLowerCase();
        
        // 图例相关
        if (name.contains("图例") || name.contains("legend")) {
            return "legend";
        }
        
        // POI相关
        if (name.contains("poi") || name.contains("配套") || name.contains("兴趣点")) {
            return "poi";
        }
        
        // 地块/几何数据相关
        if (name.contains("地块") || name.contains("轮廓") || name.contains("geometry") || name.contains("range")) {
            return "geometry";
        }
        
        // 路径相关
        if (name.contains("path") || name.contains("路径") || name.contains("路网")) {
            return "path";
        }
        
        // 文字/标签相关
        if (name.contains("文字") || name.contains("标签") || name.contains("text") || name.contains("label")) {
            return "text";
        }
        
        // 统计数据相关
        if (name.contains("统计") || name.contains("数据") || name.contains("statistics") || name.contains("data")) {
            return "statistics";
        }
        
        // 点位相关
        if (name.contains("点位") || name.contains("点") || name.contains("point")) {
            return "point";
        }
        
        // 默认返回API
        return "API";
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