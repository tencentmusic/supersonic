package com.tencent.supersonic.chat.domain.pojo.chat;


import com.tencent.supersonic.chat.api.pojo.Filter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;

@Data
public class QueryData {

    Long domainId = 0L;
    Set<SchemaItem> metrics = new HashSet<>();
    Set<SchemaItem> dimensions = new HashSet<>();
    Set<Filter> dimensionFilters = new HashSet<>();
    Set<Filter> metricFilters = new HashSet<>();
    private Set<Order> orders = new HashSet<>();
    private DateConf dateInfo;
    private Long limit;
    private Boolean nativeQuery = false;


}
