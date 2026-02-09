package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SemanticDeployParam {

    private Long databaseId;

    private boolean allowRedeploy = false;

    private Map<String, String> params = new HashMap<>();

    public String getParam(String key) {
        return params.get(key);
    }

    public String getParamOrDefault(String key, String defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }
}
