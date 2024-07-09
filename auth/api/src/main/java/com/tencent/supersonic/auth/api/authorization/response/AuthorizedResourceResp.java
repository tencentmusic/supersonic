package com.tencent.supersonic.auth.api.authorization.response;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AuthorizedResourceResp {

    private List<AuthRes> authResList = new ArrayList<>();

    private List<DimensionFilter> filters = new ArrayList<>();
}
