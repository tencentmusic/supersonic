package com.tencent.supersonic.headless.api.pojo.request;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import java.util.LinkedHashSet;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class QueryDataReq {
    private User user;
    private Set<SchemaElement> metrics = new HashSet<>();
    private Set<SchemaElement> dimensions = new HashSet<>();
    private Set<QueryFilter> dimensionFilters = new HashSet<>();
    private Set<QueryFilter> metricFilters = new HashSet<>();
    private DateConf dateInfo;
    private Set<Order> orders = new LinkedHashSet();
    private Long offset = 0L;
    private Long limit = 1000L;
    private Long queryId;
    private SemanticParseInfo parseInfo;
}
