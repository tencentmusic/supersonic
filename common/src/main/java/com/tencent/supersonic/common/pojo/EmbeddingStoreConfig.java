package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class EmbeddingStoreConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String provider;
    private String persistPath;
    private String baseUrl;
    private String apiKey;
    private Long timeOut = 60L;
    private Integer dimension;
    private String databaseName;
    private Integer post;
    private String user;
    private String password;
}
