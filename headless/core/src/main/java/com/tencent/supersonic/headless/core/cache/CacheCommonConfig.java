package com.tencent.supersonic.headless.core.cache;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class CacheCommonConfig {

    @Value("${cache.common.app:supersonic}")
    private String cacheCommonApp;

    @Value("${cache.common.env:dev}")
    private String cacheCommonEnv;

    @Value("${cache.common.version:0}")
    private Integer cacheCommonVersion;

    @Value("${cache.common.expire.after.write:10}")
    private Integer cacheCommonExpireAfterWrite;

    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;


}