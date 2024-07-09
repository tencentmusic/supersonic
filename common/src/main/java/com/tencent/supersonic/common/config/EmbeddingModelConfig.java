package com.tencent.supersonic.common.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmbeddingModelConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private String modelPath;
    private String vocabularyPath;
    private Integer maxRetries;
    private Integer maxToken;
    private Boolean logRequests;
    private Boolean logResponses;
}