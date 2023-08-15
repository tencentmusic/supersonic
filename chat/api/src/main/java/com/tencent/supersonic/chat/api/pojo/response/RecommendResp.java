package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import java.util.List;
import lombok.Data;

@Data
public class RecommendResp {

    private List<SchemaElement> dimensions;
    private List<SchemaElement> metrics;
}
