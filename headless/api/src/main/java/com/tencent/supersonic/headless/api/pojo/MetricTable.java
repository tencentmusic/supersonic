package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import lombok.Data;

import java.util.List;

@Data
public class MetricTable {

    private String alias;
    private List<String> metrics = Lists.newArrayList();
    private List<String> dimensions = Lists.newArrayList();
    private String where;
    private AggOption aggOption = AggOption.DEFAULT;

}
