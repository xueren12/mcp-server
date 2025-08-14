package com.guian.smartsite.server.service;

import com.guian.smartsite.server.config.DatabaseApiToolConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册服务 - 负责管理和注册所有MCP工具
 * 从client端迁移过来，统一在server端管理工具注册
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolRegistrationService {

    private final DatabaseApiToolConfig databaseApiToolConfig;
    private Map<String, DatabaseApiToolConfig.ApiTool> registeredTools = new HashMap<>();
    
    @PostConstruct
    public void initializeToolRegistration() {
        try {
            log.info("开始初始化MCP工具注册...");
            
            // 从数据库配置中获取已加载的MCP工具列表
            List<DatabaseApiToolConfig.ApiTool> tools = databaseApiToolConfig.getTools();
            if (tools != null && !tools.isEmpty()) {
                registeredTools.clear(); // 清空现有工具
                tools.forEach(tool -> {
                    registeredTools.put(tool.getName(), tool);
                    log.info("注册MCP工具: {} - {}", tool.getName(), tool.getDescription());
                });
                log.info("成功注册 {} 个MCP工具", tools.size());
            } else {
                log.warn("数据库中没有找到可用的API配置");
            }
        } catch (Exception e) {
            log.error("初始化MCP工具注册时发生错误", e);
        }
    }

    /**
     * 获取所有已注册的工具
     */
    public Map<String, DatabaseApiToolConfig.ApiTool> getRegisteredTools() {
        return new HashMap<>(registeredTools);
    }

    /**
     * 根据项目ID获取已注册的工具
     */
    public Map<String, DatabaseApiToolConfig.ApiTool> getRegisteredToolsByProjectId(Long projectId) {
        if (projectId == null) {
            return new HashMap<>(registeredTools);
        }
        
        Map<String, DatabaseApiToolConfig.ApiTool> filteredTools = new HashMap<>();
        registeredTools.forEach((toolName, tool) -> {
            if (projectId.equals(tool.getProjectId())) {
                filteredTools.put(toolName, tool);
            }
        });
        
        log.info("根据项目ID {} 筛选出 {} 个MCP工具", projectId, filteredTools.size());
        return filteredTools;
    }

    /**
     * 根据名称获取工具
     */
    public DatabaseApiToolConfig.ApiTool getToolByName(String toolName) {
        return registeredTools.get(toolName);
    }

    /**
     * 检查工具是否已注册
     */
    public boolean isToolRegistered(String toolName) {
        return registeredTools.containsKey(toolName);
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return registeredTools.size();
    }

    /**
     * 重新加载MCP工具注册
     */
    public void reloadTools() {
        log.info("=== 开始重新加载MCP工具注册 ===");
        
        try {
            // 记录重新加载前的状态
            int beforeCount = registeredTools.size();
            log.info("重新加载前已注册MCP工具数量: {}", beforeCount);
            if (!registeredTools.isEmpty()) {
                log.info("当前已注册MCP工具: {}", registeredTools.keySet());
            }
            
            // 重新从数据库加载MCP工具配置
            log.info("正在从数据库重新加载MCP工具配置...");
            databaseApiToolConfig.loadToolsFromDatabase();
            
            // 重新初始化MCP工具注册
            log.info("正在重新初始化MCP工具注册...");
            initializeToolRegistration();
            
            // 记录重新加载后的状态
            int afterCount = registeredTools.size();
            log.info("重新加载后已注册MCP工具数量: {}", afterCount);
            log.info("新增MCP工具数量: {}", afterCount - beforeCount);
            
            if (!registeredTools.isEmpty()) {
                log.info("重新加载后的MCP工具列表: {}", registeredTools.keySet());
            }
            
            log.info("=== MCP工具注册重新加载完成 ===");
            
        } catch (Exception e) {
            log.error("重新加载MCP工具注册时发生错误", e);
            throw new RuntimeException("重新加载MCP工具失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据工具获取详细的参数提示 - 从数据库动态生成
     */
    public String getDetailedParameterHint(String toolName) {
        DatabaseApiToolConfig.ApiTool tool = getToolByName(toolName);
        if (tool == null) {
            return "工具不存在: " + toolName;
        }
        
        // 从工具的参数配置动态生成提示
        Map<String, DatabaseApiToolConfig.ParameterInfo> parameters = tool.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            return "该工具无必需参数";
        }
        
        StringBuilder hint = new StringBuilder();
        hint.append("请提供必需参数：");
        
        parameters.entrySet().stream()
            .filter(entry -> entry.getValue().isRequired())
            .forEach(entry -> {
                String paramName = entry.getKey();
                DatabaseApiToolConfig.ParameterInfo paramInfo = entry.getValue();
                hint.append(paramName)
                    .append("（")
                    .append(paramInfo.getType())
                    .append("类型）、");
            });
        
        // 移除最后的"、"
        if (hint.toString().endsWith("、")) {
            hint.setLength(hint.length() - 1);
        }
        
        return hint.toString();
    }
}