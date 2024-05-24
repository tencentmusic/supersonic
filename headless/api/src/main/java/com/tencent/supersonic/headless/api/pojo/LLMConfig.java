package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LLMConfig {

    private String provider;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Double temperature;

    private Long timeOut;

    public LLMConfig(String provider, String baseUrl, String apiKey, String modelName) {
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.temperature = 0.0d;
        this.timeOut = 60L;
    }
}
