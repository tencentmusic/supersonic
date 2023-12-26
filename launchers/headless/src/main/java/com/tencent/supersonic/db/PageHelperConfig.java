package com.tencent.supersonic.db;

import com.github.pagehelper.PageInterceptor;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class PageHelperConfig {

    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "h2");
        pageInterceptor.setProperties(properties);
        return pageInterceptor;
    }
}
