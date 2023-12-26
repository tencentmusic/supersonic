package com.tencent.supersonic.headless.api.query.pojo;


import com.tencent.supersonic.headless.api.model.pojo.Item;
import com.tencent.supersonic.headless.api.model.response.QueryResultWithSchemaResp;
import lombok.Data;

@Data
public class ApiQuerySingleResult {

    private Item item;

    private QueryResultWithSchemaResp result;

}
