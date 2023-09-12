package com.tencent.supersonic.auth.api.authorization.request;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryAuthResReq {

    private List<String> departmentIds = new ArrayList<>();

    private List<AuthRes> resources;

    private String modelId;
}
