package com.tencent.supersonic.headless.api.pojo;


import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import lombok.Data;

@Data
public class SingleItemQueryResult {

    private Item item;

    private SemanticQueryResp result;

}
