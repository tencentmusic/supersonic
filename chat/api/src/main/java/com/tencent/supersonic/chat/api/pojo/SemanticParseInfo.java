package com.tencent.supersonic.chat.api.pojo;


import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.SchemaItem;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class SemanticParseInfo {

    String queryMode;
    AggregateTypeEnum aggType = AggregateTypeEnum.NONE;
    Long domainId = 0L;
    String domainName;
    Long entity = 0L;
    List<SchemaItem> metrics = new ArrayList<>();
    List<SchemaItem> dimensions = new ArrayList<>();
    List<Filter> dimensionFilters = new ArrayList<>();
    List<Filter> metricFilters = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private DateConf dateInfo;
    private Long limit;
    private Boolean nativeQuery = false;
}
