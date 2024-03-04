package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import lombok.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class QueryParam {
    // struct
    private List<String> groups = new ArrayList();
    private List<Aggregator> aggregators = new ArrayList();
    private List<Order> orders = new ArrayList();
    private List<Filter> dimensionFilters = new ArrayList();
    private List<Filter> metricFilters = new ArrayList();
    private DateConf dateInfo;
    private Long limit = 2000L;
    private QueryType queryType;
    private String s2SQL;
    private String correctS2SQL;
    private Long dataSetId;
    private String dataSetName;
    private Set<Long> modelIds = new HashSet<>();
    private List<Param> params = new ArrayList<>();

    // metric
    private List<String> metrics = new ArrayList();
    private List<String> dimensions;
    private String where;
    private List<ColumnOrder> order;
    private boolean nativeQuery = false;

}
