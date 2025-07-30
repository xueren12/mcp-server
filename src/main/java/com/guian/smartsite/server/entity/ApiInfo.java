package com.guian.smartsite.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

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
     * 项目id
     */
    @Column(name = "project_id", columnDefinition = "BIGINT")
    private Long projectId;
    
    /**
     * 接口描述
     */
    @Column(name = "api_desc")
    private String apiDesc;
    
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @Column(name = "del_flag")
    private String delFlag;
    
    /**
     * 创建者
     */
    @Column(name = "create_by")
    private Long createBy;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @Column(name = "update_by")
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    /**
     * 创建部门
     */
    @Column(name = "create_dept")
    private Long createDept;
    
    /**
     * 请求参数
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;
    
    /**
     * 脚本id
     */
    @Column(name = "script_id", columnDefinition = "BIGINT")
    private Long scriptId;
    
    /**
     * 返回映射脚本
     */
    @Column(name = "response_mapping_script", columnDefinition = "TEXT")
    private String responseMappingScript;
    
    /**
     * 是否分页
     */
    @Column(name = "page_setup", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean pageSetup;
    
    /**
     * 是否开启映射
     */
    @Column(name = "mapping_enable", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean mappingEnable;
    
    /**
     * 限流策略id
     */
    @Column(name = "rate_limit_rule_id", columnDefinition = "BIGINT")
    private Long rateLimitRuleId;
    
    /**
     * 缓存策略id
     */
    @Column(name = "cache_config_id", columnDefinition = "BIGINT")
    private Long cacheConfigId;
}