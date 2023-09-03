package com.tencent.supersonic.auth.api.authorization.response;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthResGrp;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class AuthorizedResourceResp {

    private List<AuthResGrp> resources = new ArrayList<>();

    private List<DimensionFilter> filters = new ArrayList<>();
}
