package com.tencent.supersonic.headless.common.core.pojo;


import com.tencent.supersonic.headless.common.server.pojo.Item;
import com.tencent.supersonic.headless.common.server.response.QueryResultWithSchemaResp;
import lombok.Data;

@Data
public class SingleItemQueryResult {

    private Item item;

    private QueryResultWithSchemaResp result;

}
