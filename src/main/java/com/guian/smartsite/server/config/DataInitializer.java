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
            log.info("开始检查API工具数据...");
            
            // 只读取数据库中现有的API工具数量
            long count = apiInfoRepository.findByProjectId(1953288277076803585L).size();
            log.info("当前数据库中有 {} 条API工具记录", count);
            
            if (count > 0) {
                log.info("数据库中已存在API工具数据，系统可以正常使用");
            } else {
                log.warn("数据库中暂无API工具数据，请联系管理员导入基础数据");
            }
            
        } catch (Exception e) {
            log.warn("读取数据库失败: {}", e.getMessage());
            log.info("应用将继续启动");
        }
    }
}