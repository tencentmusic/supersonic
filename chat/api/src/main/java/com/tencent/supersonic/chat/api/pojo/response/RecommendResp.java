package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import lombok.Data;

import java.util.List;

@Data
public class RecommendResp {
    private List<SchemaElement> dimensions;
    private List<SchemaElement> metrics;
}
