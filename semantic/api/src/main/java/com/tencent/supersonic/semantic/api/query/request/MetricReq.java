package com.tencent.supersonic.semantic.api.query.request;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MetricReq {

    private List<String> metrics;
    private List<String> dimensions;
    private String rootPath = "";
    private Map<String, String> variables;
    private String where;
    private Long limit;
    private List<ColumnOrder> order;
    private boolean nativeQuery = false;

}
