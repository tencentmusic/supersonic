package com.tencent.supersonic.headless.core.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class LLMParserConfig {


    @Value("${llm.parser.url:}")
    private String url;

    @Value("${query2sql.path:/query2sql}")
    private String queryToSqlPath;

    @Value("${dimension.topn:10}")
    private Integer dimensionTopN;

    @Value("${metric.topn:10}")
    private Integer metricTopN;

    @Value("${tag.topn:20}")
    private Integer tagTopN;

    @Value("${all.model:false}")
    private Boolean allModel;
}
