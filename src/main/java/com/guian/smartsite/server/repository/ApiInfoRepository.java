package com.guian.smartsite.server.repository;

import com.guian.smartsite.server.entity.ApiInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
     * 根据项目ID查询API信息
     * @param projectId 项目ID
     * @return API信息列表
     */
    @Query("SELECT a FROM ApiInfo a WHERE a.projectId = :projectId AND a.delFlag = '0'")
    List<ApiInfo> findByProjectId(Long projectId);
    
    /**
     * 根据API名称查询
     * @param apiName API名称
     * @return API信息
     */
    @Query("SELECT a FROM ApiInfo a WHERE a.apiName = :apiName AND a.delFlag = '0'")
    ApiInfo findByApiName(String apiName);
    
    /**
     * 根据API类型查询
     * @param apiType API类型
     * @return API信息列表
     */
    @Query("SELECT a FROM ApiInfo a WHERE a.apiType = :apiType AND a.delFlag = '0'")
    List<ApiInfo> findByApiType(String apiType);
}