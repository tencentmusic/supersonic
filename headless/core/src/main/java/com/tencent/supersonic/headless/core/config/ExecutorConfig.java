package com.tencent.supersonic.headless.core.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ExecutorConfig {

    @Value("${s2.metricParser.agg.mysql.lowVersion:8.0}")
    private String mysqlLowVersion;

    @Value("${s2.metricParser.agg.ck.lowVersion:20.4}")
    private String ckLowVersion;

    @Value("${s2.internal.metric.cnt.suffix:internal_cnt}")
    private String internalMetricNameSuffix;

    @Value("${s2.accelerator.duckDb.enable:false}")
    private Boolean duckEnable = false;

    @Value("${s2.accelerator.duckDb.temp:/data1/duck/tmp/}")
    private String duckDbTemp;

    @Value("${s2.accelerator.duckDb.maximumPoolSize:10}")
    private Integer duckDbMaximumPoolSize;

    @Value("${s2.accelerator.duckDb.MaxLifetime:3}")
    private Integer duckDbMaxLifetime;

    @Value("${s2.accelerator.duckDb.memoryLimit:31}")
    private Integer memoryLimit;

    @Value("${s2.accelerator.duckDb.threads:32}")
    private Integer threads;
}
