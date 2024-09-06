package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class SchemaFilterReq {

    private Long dataSetId;

    private List<Long> modelIds = Lists.newArrayList();
}
