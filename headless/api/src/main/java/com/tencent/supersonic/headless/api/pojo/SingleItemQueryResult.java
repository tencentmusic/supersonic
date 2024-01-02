package com.tencent.supersonic.headless.api.pojo;


import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import lombok.Data;

@Data
public class SingleItemQueryResult {

    private Item item;

    private QueryResultWithSchemaResp result;

}
