package com.tencent.supersonic.auth.api.authorization.pojo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AuthRes {

    private String modelId;
    private String name;

    public AuthRes() {
    }

    public AuthRes(String modelId, String name) {
        this.modelId = modelId;
        this.name = name;
    }
}
