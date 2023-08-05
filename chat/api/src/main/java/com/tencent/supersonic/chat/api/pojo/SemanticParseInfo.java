package com.tencent.supersonic.chat.api.pojo;


import java.util.*;

import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import lombok.Data;

@Data
public class SemanticParseInfo {

    private String queryMode;
    private SchemaElement domain;
    private Set<SchemaElement> metrics = new TreeSet<>(new SchemaNameLengthComparator());
    private Set<SchemaElement> dimensions = new LinkedHashSet();
    private SchemaElement entity;
    private AggregateTypeEnum aggType = AggregateTypeEnum.NONE;
    private Set<QueryFilter> dimensionFilters = new LinkedHashSet();
    private Set<QueryFilter> metricFilters = new LinkedHashSet();
    private Set<Order> orders = new LinkedHashSet();
    private DateConf dateInfo;
    private Long limit;
    private Boolean nativeQuery = false;
    private double score;
    private List<SchemaElementMatch> elementMatches = new ArrayList<>();
    private Map<String, Object> properties = new HashMap<>();

    public Long getDomainId() {
        return domain != null ? domain.getId() : 0L;
    }

    public String getDomainName() {
        return domain != null ? domain.getName() : "null";
    }

    private static class SchemaNameLengthComparator implements Comparator<SchemaElement> {
        @Override
        public int compare(SchemaElement o1, SchemaElement o2) {
            int len1 = o1.getName().length();
            int len2 = o2.getName().length();
            if (len1 != len2) {
                return len1 - len2;
            } else {
                return o1.getName().compareTo(o2.getName());
            }
        }
    }

}
