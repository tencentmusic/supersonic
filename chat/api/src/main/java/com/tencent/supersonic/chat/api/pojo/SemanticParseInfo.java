package com.tencent.supersonic.chat.api.pojo;


import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.SchemaItem;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class SemanticParseInfo {

    String queryMode;
    AggregateTypeEnum aggType = AggregateTypeEnum.NONE;
    Long domainId = 0L;
    String domainName;
    Long entity = 0L;
    Set<SchemaItem> metrics = new LinkedHashSet();
    Set<SchemaItem> dimensions = new LinkedHashSet();
    Set<Filter> dimensionFilters = new LinkedHashSet();
    Set<Filter> metricFilters = new LinkedHashSet();
    private Set<Order> orders = new LinkedHashSet();
    private DateConf dateInfo;
    private Long limit;
    private Boolean nativeQuery = false;
}
