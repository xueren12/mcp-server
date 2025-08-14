package com.guian.smartsite.server.controller;

import com.guian.smartsite.server.service.ToolRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP工具管理控制器
 * 提供MCP工具的动态管理功能，包括重新加载、查看状态等
 */
@RestController
@RequestMapping("/api/tools")
@Slf4j
@RequiredArgsConstructor
public class ToolManagementController {

    private final ToolRegistrationService toolRegistrationService;

    /**
     * 重新加载MCP工具
     * 当数据库中新增MCP工具或修改mcp_flag时，调用此接口可以动态加载新工具，无需重启服务器
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadTools() {
        try {
            log.info("收到MCP工具重新加载请求");
            
            // 记录重新加载前的MCP工具数量
            int beforeCount = toolRegistrationService.getToolCount();
            log.info("重新加载前MCP工具数量: {}", beforeCount);
            
            // 执行重新加载
            toolRegistrationService.reloadTools();
            
            // 记录重新加载后的MCP工具数量
            int afterCount = toolRegistrationService.getToolCount();
            log.info("重新加载后MCP工具数量: {}", afterCount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MCP工具重新加载成功");
            response.put("beforeCount", beforeCount);
            response.put("afterCount", afterCount);
            response.put("newToolsAdded", afterCount - beforeCount);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("MCP工具重新加载成功，新增MCP工具数量: {}", afterCount - beforeCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("MCP工具重新加载失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "MCP工具重新加载失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取MCP工具状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getToolStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("totalMcpTools", toolRegistrationService.getToolCount());
            status.put("registeredMcpTools", toolRegistrationService.getRegisteredTools().keySet());
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取MCP工具状态失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取MCP工具状态失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取指定MCP工具的详细信息
     */
    @GetMapping("/info/{toolName}")
    public ResponseEntity<Map<String, Object>> getToolInfo(@PathVariable String toolName) {
        try {
            if (!toolRegistrationService.isToolRegistered(toolName)) {
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put("success", false);
                notFoundResponse.put("message", "MCP工具 '" + toolName + "' 未找到");
                notFoundResponse.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.notFound().build();
            }
            
            // 获取 ApiTool 对象并转换为 Map
            var apiTool = toolRegistrationService.getToolByName(toolName);
            Map<String, Object> toolInfo = convertApiToolToMap(apiTool);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mcpToolInfo", toolInfo);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取MCP工具信息失败: {}", toolName, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取MCP工具信息失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 将 ApiTool 对象转换为 Map 格式
     */
    private Map<String, Object> convertApiToolToMap(com.guian.smartsite.server.config.DatabaseApiToolConfig.ApiTool apiTool) {
        Map<String, Object> toolMap = new HashMap<>();
        toolMap.put("name", apiTool.getName());
        toolMap.put("description", apiTool.getDescription());
        toolMap.put("url", apiTool.getUrl());
        toolMap.put("method", apiTool.getMethod());
        toolMap.put("apiType", apiTool.getApiType());
        toolMap.put("mockData", apiTool.getMockData());
        toolMap.put("dataType", apiTool.getDataType());
        toolMap.put("headers", apiTool.getHeaders());
        toolMap.put("parameters", apiTool.getParameters());
        toolMap.put("parameterHint", toolRegistrationService.getDetailedParameterHint(apiTool.getName()));
        return toolMap;
    }

    /**
     * MCP工具管理服务健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "MCP Tool Management Service");
        health.put("totalMcpTools", toolRegistrationService.getToolCount());
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
}