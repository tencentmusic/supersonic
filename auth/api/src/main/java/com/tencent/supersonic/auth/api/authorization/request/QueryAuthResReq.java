package com.tencent.supersonic.auth.api.authorization.request;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryAuthResReq {

    private String user;
    private List<AuthRes> resources;
    private String domainId;
}
