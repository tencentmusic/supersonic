package com.tencent.supersonic.chat.api.pojo.request;


import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
public class QueryDataReq {
    String queryMode;
    SchemaElement model;
    Set<SchemaElement> metrics = new HashSet<>();
    Set<SchemaElement> dimensions = new HashSet<>();
    Set<QueryFilter> dimensionFilters = new HashSet<>();
    Set<QueryFilter> metricFilters = new HashSet<>();
    private AggregateTypeEnum aggType = AggregateTypeEnum.NONE;
    private Set<Order> orders = new HashSet<>();
    private DateConf dateInfo;
    private Long limit;
    private Boolean nativeQuery = false;
}
