package dev.langchain4j.pgvector.spring;

import dev.langchain4j.store.embedding.pgvector.MetadataStorageConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EmbeddingStoreProperties {

    private String host;
    private Integer port;
    private String user;
    private String password;
    private String database;
    private String table;
    private Integer dimension;
    private Boolean useIndex;
    private Integer indexListSize;
    private Boolean createTable;
    private Boolean dropTableFirst;
    private MetadataStorageConfig metadataStorageConfig;
}
