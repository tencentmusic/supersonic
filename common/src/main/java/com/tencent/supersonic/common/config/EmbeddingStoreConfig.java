package com.tencent.supersonic.common.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmbeddingStoreConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String provider;
    private String persistPath;
    private String baseUrl;
    private String apiKey;
    private Long timeOut = 60L;
}