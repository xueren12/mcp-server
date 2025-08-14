package com.guian.smartsite.server.repository;

import com.guian.smartsite.server.entity.ApiInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * API信息Repository接口
 */
@Repository
public interface ApiInfoRepository extends JpaRepository<ApiInfo, Long> {
    
    /**
     * 查询所有未删除的API信息
     * @return API信息列表
     */
    @Query("SELECT a FROM ApiInfo a WHERE a.delFlag = '0'")
    List<ApiInfo> findAllActive();
    
    /**
     * 根据项目ID查询所有可用的API信息（用于注册为MCP工具）
     * @param projectId 项目ID
     * @return API信息列表
     */
    @Query("SELECT a FROM ApiInfo a WHERE a.projectId = :projectId AND a.delFlag = '0'")
    List<ApiInfo> findByProjectId(Long projectId);
    
    /**
     * 查询所有已注册为MCP工具的API信息
     * @return API信息列表
     */
    @Query("SELECT a FROM ApiInfo a WHERE a.delFlag = '0' AND a.mcpFlag = '0'")
    List<ApiInfo> findAllMcpTools();
    
    /**
     * 批量更新API的mcp_flag为0（标记为已注册MCP工具）
     * @param apiIds API ID列表
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE ApiInfo a SET a.mcpFlag = '0' WHERE a.apiId IN :apiIds")
    int updateMcpFlagToRegistered(List<Long> apiIds);
    
    /**
     * 根据API ID更新单个API的mcp_flag为0
     * @param apiId API ID
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE ApiInfo a SET a.mcpFlag = '0' WHERE a.apiId = :apiId")
    int updateSingleApiMcpFlag(Long apiId);

}