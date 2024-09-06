package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SingleItemQueryResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ItemQueryResultResp {

    private List<SingleItemQueryResult> results;
}
