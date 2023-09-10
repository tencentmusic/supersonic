package com.tencent.supersonic.chat.parser.plugin.embedding;

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

}
