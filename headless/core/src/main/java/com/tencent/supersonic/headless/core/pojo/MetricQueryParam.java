package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MetricQueryParam {

    private List<String> metrics;
    private List<String> dimensions;
    private Map<String, String> variables;
    private String where;
    private Long limit;
    private List<ColumnOrder> order;
    private boolean nativeQuery = false;

}
