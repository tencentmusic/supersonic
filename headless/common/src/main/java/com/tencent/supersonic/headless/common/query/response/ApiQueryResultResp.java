package com.tencent.supersonic.headless.common.query.response;

import com.tencent.supersonic.headless.common.query.pojo.ApiQuerySingleResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApiQueryResultResp {

    private List<ApiQuerySingleResult> results;

}
