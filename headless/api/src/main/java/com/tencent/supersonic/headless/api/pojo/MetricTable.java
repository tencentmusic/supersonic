package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.headless.api.enums.AggOption;
import lombok.Data;

import java.util.List;

@Data
public class MetricTable {

    private String alias;
    private List<String> metrics;
    private List<String> dimensions;
    private String where;
    private AggOption aggOption = AggOption.DEFAULT;

}
