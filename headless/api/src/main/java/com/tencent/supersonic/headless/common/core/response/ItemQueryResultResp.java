package com.tencent.supersonic.headless.common.core.response;

import com.tencent.supersonic.headless.common.core.pojo.SingleItemQueryResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ItemQueryResultResp {

    private List<SingleItemQueryResult> results;

}
