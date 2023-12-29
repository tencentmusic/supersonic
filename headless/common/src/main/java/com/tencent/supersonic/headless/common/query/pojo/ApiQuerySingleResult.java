package com.tencent.supersonic.headless.common.query.pojo;


import com.tencent.supersonic.headless.common.model.pojo.Item;
import com.tencent.supersonic.headless.common.model.response.QueryResultWithSchemaResp;
import lombok.Data;

@Data
public class ApiQuerySingleResult {

    private Item item;

    private QueryResultWithSchemaResp result;

}
