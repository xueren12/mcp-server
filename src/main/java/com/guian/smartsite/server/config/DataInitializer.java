package com.guian.smartsite.server.config;

import com.guian.smartsite.server.repository.ApiInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * 数据初始化器 - 确保基本的API工具数据存在
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final ApiInfoRepository apiInfoRepository;
    
    @Override
    public void run(String... args) {
        try {
            log.info("开始检查可用的API数据...");
            
            // 读取数据库中所有可用的API数量
            long count = apiInfoRepository.findByProjectId(1953288277076803585L).size();
            log.info("当前数据库中有 {} 条可用的API记录", count);
            
            if (count > 0) {
                log.info("数据库中已存在API数据，系统启动时将自动注册为MCP工具");
            } else {
                log.warn("数据库中暂无可用的API数据，请联系管理员添加API配置");
            }
            
        } catch (Exception e) {
            log.warn("读取数据库失败: {}", e.getMessage());
            log.info("应用将继续启动");
        }
    }
}