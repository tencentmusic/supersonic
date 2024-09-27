package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterType;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import lombok.Data;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.tencent.supersonic.common.pojo.Constants.DEFAULT_DETAIL_LIMIT;
import static com.tencent.supersonic.common.pojo.Constants.DEFAULT_METRIC_LIMIT;

@Data
public class SemanticParseInfo {

    private Integer id;
    private String queryMode = "PLAIN_TEXT";
    private SchemaElement dataSet;
    private QueryConfig queryConfig;
    private Set<SchemaElement> metrics = Sets.newTreeSet(new SchemaNameLengthComparator());
    private Set<SchemaElement> dimensions = Sets.newTreeSet(new SchemaNameLengthComparator());
    private SchemaElement entity;
    private AggregateTypeEnum aggType = AggregateTypeEnum.NONE;
    private FilterType filterType = FilterType.AND;
    private Set<QueryFilter> dimensionFilters = Sets.newHashSet();
    private Set<QueryFilter> metricFilters = Sets.newHashSet();
    private Set<Order> orders = Sets.newHashSet();
    private DateConf dateInfo;
    private long limit = DEFAULT_DETAIL_LIMIT;
    private double score;
    private List<SchemaElementMatch> elementMatches = Lists.newArrayList();
    private SqlInfo sqlInfo = new SqlInfo();
    private SqlEvaluation sqlEvaluation = new SqlEvaluation();
    private QueryType queryType = QueryType.ID;
    private EntityInfo entityInfo;
    private String textInfo;
    private Map<String, Object> properties = Maps.newHashMap();

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

    public Long getDataSetId() {
        if (dataSet == null) {
            return null;
        }
        return dataSet.getDataSetId();
    }

    public long getDetailLimit() {
        long limit = DEFAULT_DETAIL_LIMIT;
        if (Objects.nonNull(queryConfig)
                && Objects.nonNull(queryConfig.getDetailTypeDefaultConfig())
                && Objects.nonNull(queryConfig.getDetailTypeDefaultConfig().getLimit())) {
            limit = queryConfig.getDetailTypeDefaultConfig().getLimit();
        }
        return limit;
    }

    public long getMetricLimit() {
        long limit = DEFAULT_METRIC_LIMIT;
        if (Objects.nonNull(queryConfig)
                && Objects.nonNull(queryConfig.getAggregateTypeDefaultConfig())
                && Objects.nonNull(queryConfig.getAggregateTypeDefaultConfig().getLimit())) {
            limit = queryConfig.getAggregateTypeDefaultConfig().getLimit();
        }
        return limit;
    }
}
