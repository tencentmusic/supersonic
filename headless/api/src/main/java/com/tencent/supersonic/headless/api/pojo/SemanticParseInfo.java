package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterType;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Data
public class SemanticParseInfo {

    private Integer id;
    private String queryMode = "PLAIN_TEXT";
    private SchemaElement dataSet;
    private Set<SchemaElement> metrics = new TreeSet<>(new SchemaNameLengthComparator());
    private Set<SchemaElement> dimensions = new LinkedHashSet();
    private SchemaElement entity;
    private AggregateTypeEnum aggType = AggregateTypeEnum.NONE;
    private FilterType filterType = FilterType.AND;
    private Set<QueryFilter> dimensionFilters = new LinkedHashSet();
    private Set<QueryFilter> metricFilters = new LinkedHashSet();
    private Set<Order> orders = new LinkedHashSet();
    private DateConf dateInfo;
    private Long limit;
    private double score;
    private List<SchemaElementMatch> elementMatches = new ArrayList<>();
    private Map<String, Object> properties = new HashMap<>();
    private SqlInfo sqlInfo = new SqlInfo();
    private SqlEvaluation sqlEvaluation = new SqlEvaluation();
    private QueryType queryType = QueryType.ID;
    private EntityInfo entityInfo;
    private String textInfo;
    private static class SchemaNameLengthComparator implements Comparator<SchemaElement> {

        @Override
        public int compare(SchemaElement o1, SchemaElement o2) {
            if (o1.getOrder() != o2.getOrder()) {
                if (o1.getOrder() < o2.getOrder()) {
                    return -1;
                } else {
                    return 1;
                }
            }
            int len1 = o1.getName().length();
            int len2 = o2.getName().length();
            if (len1 != len2) {
                return len1 - len2;
            } else {
                return o1.getName().compareTo(o2.getName());
            }
        }
    }

    public Set<SchemaElement> getMetrics() {
        Set<SchemaElement> metricSet = new TreeSet<>(new SchemaNameLengthComparator());
        metricSet.addAll(metrics);
        metrics = metricSet;
        return metrics;
    }

    public Long getDataSetId() {
        if (dataSet == null) {
            return null;
        }
        return dataSet.getDataSet();
    }

}
