package com.guian.smartsite.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * API信息表实体类
 */
@Entity
@Table(name = "api_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiInfo {
    
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "api_id")
    private Long apiId;
    
    /**
     * 接口名称
     */
    @Column(name = "api_name")
    private String apiName;
    
    /**
     * 接口路径
     */
    @Column(name = "api_path")
    private String apiPath;
    
    /**
     * 接口类型：sql、mock
     */
    @Column(name = "api_type")
    private String apiType;
    
    /**
     * 请求方式 get、post、put、delete、patch
     */
    @Column(name = "api_method")
    private String apiMethod;
    
    /**
     * 授权类型：none、code、secret
     */
    @Column(name = "auth_type")
    private String authType;
    
    /**
     * SQL数据
     */
    @Column(name = "sql_data", columnDefinition = "TEXT")
    private String sqlData;
    
    /**
     * MOCK数据
     */
    @Column(name = "mock_data", columnDefinition = "TEXT")
    private String mockData;
    
    /**
     * 数据源编码
     */
    @Column(name = "datasource_code")
    private String datasourceCode;
    
    /**
     * 数据源类型
     */
    @Column(name = "datasource_type")
    private String datasourceType;
    
    /**
     * 项目ID
     */
    @Column(name = "project_id", columnDefinition = "BIGINT")
    private Long projectId;
    
    /**
     * 接口描述
     */
    @Column(name = "api_desc")
    private String apiDesc;
    
    /**
     * 删除标志
     */
    @Column(name = "del_flag")
    private String delFlag;
    
    /**
     * 请求参数
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;
    
    /**
     * 数据类型
     */
    @Column(name = "data_type")
    private String dataType = "API";
    
    /**
     * MCP工具标志：0-已注册为MCP工具，2-未注册为MCP工具
     */
    @Column(name = "mcp_flag")
    private String mcpFlag = "2";
}