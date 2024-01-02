package com.tencent.supersonic.headless.api.request;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MetricQueryReq {

    private List<String> metrics;
    private List<String> dimensions;
    private String rootPath = "";
    private Map<String, String> variables;
    private String where;
    private Long limit;
    private List<ColumnOrder> order;
    private boolean nativeQuery = false;

}
