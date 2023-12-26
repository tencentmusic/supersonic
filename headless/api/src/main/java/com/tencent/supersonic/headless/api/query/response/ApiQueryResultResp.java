package com.tencent.supersonic.headless.api.query.response;

import com.tencent.supersonic.headless.api.query.pojo.ApiQuerySingleResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApiQueryResultResp {

    private List<ApiQuerySingleResult> results;

}
