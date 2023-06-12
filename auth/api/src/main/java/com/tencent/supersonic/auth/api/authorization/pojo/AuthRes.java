package com.tencent.supersonic.auth.api.authorization.pojo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AuthRes {

    private String domainId;
    private String name;

    public AuthRes() {
    }

    public AuthRes(String domainId, String name) {
        this.domainId = domainId;
        this.name = name;
    }
}
