package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.ValueDistribution;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class ItemValueResp {
    private SchemaElementType type;
    private Long itemId;
    private String name;
    private String bizName;
    private List<ValueDistribution> valueDistributionList;
}