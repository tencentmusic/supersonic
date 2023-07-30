package com.tencent.supersonic.chat.api.pojo;


import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class SemanticParseInfo {

    String queryMode;
    SchemaElement domain;
    Set<SchemaElement> metrics = new LinkedHashSet();
    Set<SchemaElement> dimensions = new LinkedHashSet();
    Long entity = 0L;
    AggregateTypeEnum aggType = AggregateTypeEnum.NONE;
    Set<QueryFilter> dimensionFilters = new LinkedHashSet();
    Set<QueryFilter> metricFilters = new LinkedHashSet();
    private Set<Order> orders = new LinkedHashSet();
    private DateConf dateInfo;
    private Long limit;
    private Boolean nativeQuery = false;
    private Double bonus = 0d;
    private List<SchemaElementMatch> elementMatches = new ArrayList<>();
    private Map<String, Object> properties;

    public Long getDomainId() {
        return domain != null ? domain.getId() : 0L;
    }

    public String getDomainName() {
        return domain != null ? domain.getName() : "null";
    }

    public Set<SchemaElement> getMetrics() {
        this.metrics = this.metrics.stream().sorted((o1, o2) -> {
            int len1 = o1.getName().length();
            int len2 = o2.getName().length();
            if (len1 != len2) {
                return len1 - len2;
            } else {
                return o1.getName().compareTo(o2.getName());
            }
        }).collect(Collectors.toCollection(LinkedHashSet::new));
        return this.metrics;
    }
}
