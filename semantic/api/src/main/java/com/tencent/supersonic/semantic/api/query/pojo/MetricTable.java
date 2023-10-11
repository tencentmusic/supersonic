package com.tencent.supersonic.semantic.api.query.pojo;

import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import java.util.List;
import lombok.Data;

@Data
public class MetricTable {

    private String alias;
    private List<String> metrics;
    private List<String> dimensions;
    private String where;
    private AggOption aggOption = AggOption.DEFAULT;

}
