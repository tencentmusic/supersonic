package dev.langchain4j.milvus.spring;

import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EmbeddingStoreProperties {

    private String uri;
    private String host;
    private Integer port;
    private String collectionName;
    private Integer dimension;
    private IndexType indexType;
    private MetricType metricType;
    private String token;
    private String user;
    private String password;
    private ConsistencyLevelEnum consistencyLevel;
    private Boolean retrieveEmbeddingsOnSearch;
    private String databaseName;
    private Boolean autoFlushOnInsert;
}
