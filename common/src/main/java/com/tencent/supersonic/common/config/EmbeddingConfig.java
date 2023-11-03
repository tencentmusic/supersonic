package com.tencent.supersonic.common.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class EmbeddingConfig {

    @Value("${embedding.url:}")
    private String url;

    @Value("${embedding.recognize.path:/preset_query_retrival}")
    private String recognizePath;

    @Value("${embedding.delete.path:/preset_delete_by_ids}")
    private String deletePath;

    @Value("${embedding.add.path:/preset_query_add}")
    private String addPath;

    @Value("${embedding.nResult:1}")
    private String nResult;

    @Value("${embedding.solvedQuery.recall.path:/solved_query_retrival}")
    private String solvedQueryRecallPath;

    @Value("${embedding.solvedQuery.add.path:/solved_query_add}")
    private String solvedQueryAddPath;

    @Value("${embedding.solved.query.nResult:5}")
    private String solvedQueryResultNum;

}
