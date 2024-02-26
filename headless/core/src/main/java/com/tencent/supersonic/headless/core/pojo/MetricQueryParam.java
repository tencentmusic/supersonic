package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import lombok.Data;
import java.util.List;

@Data
public class MetricQueryParam {

    private List<String> metrics;
    private List<String> dimensions;
    private String where;
    private Long limit;
    private List<ColumnOrder> order;
    private boolean nativeQuery = false;

}
