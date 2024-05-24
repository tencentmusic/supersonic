package com.tencent.supersonic.headless.core.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class LLMParserConfig {

    @Value("${s2.parser.url:}")
    private String url;

    @Value("${s2.query2sql.path:/query2sql}")
    private String queryToSqlPath;

    @Value("${s2.dimension.topn:10}")
    private Integer dimensionTopN;

    @Value("${s2.metric.topn:10}")
    private Integer metricTopN;

    @Value("${s2.tag.topn:20}")
    private Integer tagTopN;

    @Value("${s2.all.model:false}")
    private Boolean allModel;
}
