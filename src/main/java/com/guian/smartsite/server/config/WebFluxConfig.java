package com.guian.smartsite.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux配置类 - 解决大数据量传输问题
 */
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
    }
}