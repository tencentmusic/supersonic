package com.tencent.supersonic.headless.chat.parser.llm;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class LLMParserConfig {
    @Value("${s2.recall.max.retries:3}")
    private int recallMaxRetries;

    @Value("${s2.dimension.topn:10}")
    private Integer dimensionTopN;

    @Value("${s2.metric.topn:10}")
    private Integer metricTopN;

    @Value("${s2.tag.topn:20}")
    private Integer tagTopN;

    @Value("${s2.all.model:false}")
    private Boolean allModel;
}
