package com.tencent.supersonic.headless.core.cache;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class CacheCommonConfig {

    @Value("${s2.cache.common.app:supersonic}")
    private String cacheCommonApp;

    @Value("${s2.cache.common.env:dev}")
    private String cacheCommonEnv;

    @Value("${s2.cache.common.version:0}")
    private Integer cacheCommonVersion;

    @Value("${s2.cache.common.expire.after.write:10}")
    private Integer cacheCommonExpireAfterWrite;

    @Value("${s2.query.cache.enable:true}")
    private Boolean cacheEnable;


}