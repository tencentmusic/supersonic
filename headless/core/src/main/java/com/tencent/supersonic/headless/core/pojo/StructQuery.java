package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.Param;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StructQuery {
    private List<String> groups = new ArrayList();
    private List<Aggregator> aggregators = new ArrayList();
    private List<Order> orders = new ArrayList();
    private List<Filter> dimensionFilters = new ArrayList();
    private List<Filter> metricFilters = new ArrayList();
    private DateConf dateInfo;
    private Long limit = 2000L;
    private QueryType queryType;
    private List<Param> params = new ArrayList<>();
}
