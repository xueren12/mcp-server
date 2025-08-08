package com.guian.smartsite.server.controller;

import com.guian.smartsite.server.service.DynamicToolService;
import com.guian.smartsite.server.config.DatabaseApiToolConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * MCP协议控制器 - 实现MCP JSON-RPC 2.0协议
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class McpProtocolController {

    private final DynamicToolService dynamicToolService;
    private final DatabaseApiToolConfig databaseApiToolConfig;


    /**
     * MCP协议端点 - 处理JSON-RPC 2.0请求
     */
    @PostMapping("/mcp")
    public Mono<ResponseEntity<Map<String, Object>>> handleMcpRequest(@RequestBody Map<String, Object> request) {
        log.info("收到MCP协议请求: {}", request);
        
        try {
            String jsonrpc = (String) request.get("jsonrpc");
            Object id = request.get("id");
            String method = (String) request.get("method");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            
            // 验证JSON-RPC 2.0格式
            if (!"2.0".equals(jsonrpc)) {
                return Mono.just(ResponseEntity.ok(createErrorResponse(id, -32600, "Invalid Request - 不是有效的JSON-RPC 2.0请求")));
            }
            
            // 处理不同的MCP方法
            switch (method) {
                case "initialize":
                    return Mono.just(handleInitialize(id, params));
                
                case "tools/list":
                    return Mono.just(handleToolsList(id));
                
                case "tools/call":
                    return handleToolsCall(id, params);
                
                default:
                    return Mono.just(ResponseEntity.ok(createErrorResponse(id, -32601, "Method not found - 未知方法: " + method)));
            }
            
        } catch (Exception e) {
            log.error("处理MCP请求失败", e);
            return Mono.just(ResponseEntity.ok(createErrorResponse(null, -32603, "Internal error - 内部错误: " + e.getMessage())));
        }
    }
    
    /**
     * 处理初始化请求
     */
    private ResponseEntity<Map<String, Object>> handleInitialize(Object id, Map<String, Object> params) {
        log.info("处理MCP初始化请求");
        
        Map<String, Object> result = Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of(),
                "resources", Map.of(),
                "prompts", Map.of()
            ),
            "serverInfo", Map.of(
                "name", "贵安智能选址MCP服务器",
                "version", "1.0.0",
                "description", "提供贵安新区智慧工地SHP数据样式服务"
            )
        );
        
        return ResponseEntity.ok(createSuccessResponse(id, result));
    }
    
    /**
     * 处理工具列表请求 - 动态生成工具列表
     */
    private ResponseEntity<Map<String, Object>> handleToolsList(Object id) {
        log.info("处理工具列表请求");
        
        List<Map<String, Object>> tools = new ArrayList<>();
        
        // 添加所有数据库中的工具
        if (databaseApiToolConfig.getTools() != null) {
            for (DatabaseApiToolConfig.ApiTool tool : databaseApiToolConfig.getTools()) {
                Map<String, Object> properties = new HashMap<>();
                List<String> required = new ArrayList<>();
                
                if (tool.getParameters() != null) {
                    tool.getParameters().forEach((paramName, paramInfo) -> {
                        if (paramInfo != null) {
                            String type = paramInfo.getType() != null ? paramInfo.getType() : "string";
                            String description = paramInfo.getDescription() != null ? paramInfo.getDescription() : "";
                            
                            properties.put(paramName, Map.of(
                                "type", type,
                                "description", description
                            ));
                            if (paramInfo.isRequired()) {
                                required.add(paramName);
                            }
                        }
                    });
                }
                
                tools.add(Map.of(
                    "name", tool.getName() != null ? tool.getName() : "unknown",
                    "description", tool.getDescription() != null ? tool.getDescription() : "No description available",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", properties,
                        "required", required
                    )
                ));
            }
        }
        
        Map<String, Object> result = Map.of("tools", tools);
        return ResponseEntity.ok(createSuccessResponse(id, result));
    }

    /**
     * 处理工具调用请求
     */
    private Mono<ResponseEntity<Map<String, Object>>> handleToolsCall(Object id, Map<String, Object> params) {
        log.info("=== MCP协议工具调用开始 ===");
        log.info("处理工具调用请求: {}", params);
        
        try {
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
            
            log.info("工具名称: {}, 参数: {}", toolName, arguments);
            
            if (toolName == null) {
                log.error("工具名称为空");
                return Mono.just(ResponseEntity.badRequest().body(createErrorResponse(id, -32602, "Missing tool name")));
            }
            
            // 统一使用 DynamicToolService 处理所有工具调用 - 使用响应式方式
            log.info("调用 DynamicToolService.callApiWithType: {}", toolName);
            return dynamicToolService.callApiWithType(toolName, arguments != null ? arguments : Map.of())
                .map(result -> {
                    log.info("=== DynamicToolService 返回结果 ===");
                    log.info("结果长度: {}", result != null ? result.length() : 0);
                    log.info("结果内容前200字符: {}", result != null && result.length() > 200 ? result.substring(0, 200) + "..." : result);
                    
                    Map<String, Object> content = Map.of(
                        "type", "text",
                        "text", result
                    );
                    
                    Map<String, Object> responseResult = Map.of("content", List.of(content));
                    log.info("=== MCP协议响应构建完成 ===");
                    return ResponseEntity.ok(createSuccessResponse(id, responseResult));
                })
                .onErrorResume(e -> {
                    log.error("工具调用失败", e);
                    return Mono.just(ResponseEntity.status(500).body(createErrorResponse(id, -32603, "Tool call failed: " + e.getMessage())));
                });
            
        } catch (Exception e) {
            log.error("工具调用失败", e);
            return Mono.just(ResponseEntity.status(500).body(createErrorResponse(id, -32603, "Tool call failed: " + e.getMessage())));
        }
    }
    
    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(Object id, Object result) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "result", result
        );
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(Object id, int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id); // 允许 null 值
        response.put("error", Map.of(
            "code", code,
            "message", message != null ? message : "Unknown error"
        ));
        return response;
    }
    
    /**
     * MCP协议健康检查
     */
    @GetMapping("/mcp/health")
    public ResponseEntity<Map<String, Object>> mcpHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "protocol", "MCP JSON-RPC 2.0",
            "service", "贵安智能选址MCP服务器",
            "version", "1.0.0",
            "capabilities", List.of("tools")
        ));
    }
}