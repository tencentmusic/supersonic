package com.tencent.supersonic.headless.core.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ExecutorConfig {

    @Value("${metricParser.agg.mysql.lowVersion:5.7}")
    private String mysqlLowVersion;
    @Value("${metricParser.agg.ck.lowVersion:20.4}")
    private String ckLowVersion;
    @Value("${internal.metric.cnt.suffix:internal_cnt}")
    private String internalMetricNameSuffix;

    @Value("${accelerator.duckDb.enable:false}")
    private Boolean duckEnable = false;

    @Value("${accelerator.duckDb.temp:/data1/duck/tmp/}")
    private String duckDbTemp;

    @Value("${accelerator.duckDb.maximumPoolSize:10}")
    private Integer duckDbMaximumPoolSize;

    @Value("${accelerator.duckDb.MaxLifetime:3}")
    private Integer duckDbMaxLifetime;

    @Value("${accelerator.duckDb.memoryLimit:31}")
    private Integer memoryLimit;

    @Value("${accelerator.duckDb.threads:32}")
    private Integer threads;
}
