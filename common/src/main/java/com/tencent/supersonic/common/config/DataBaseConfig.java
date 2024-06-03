package com.tencent.supersonic.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@Primary
public class DataBaseConfig {

    @Bean("h2")
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return new DruidDataSource();
    }

}
