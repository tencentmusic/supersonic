package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@ToString
public class QueryTagReq extends SemanticQueryReq {

    private List<String> groups = new ArrayList<>();
    private List<Aggregator> aggregators = new ArrayList<>();
    private List<Filter> tagFilters = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();

    private Long limit = 20L;
    private Long offset = 0L;

    private String tagFiltersDate;
    private DateConf dateInfo;

    @Override
    public String toCustomizedString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"dataSetId\":").append(dataSetId);
        stringBuilder.append("\"modelIds\":").append(modelIds);
        stringBuilder.append(",\"groups\":").append(groups);
        stringBuilder.append(",\"aggregators\":").append(aggregators);
        stringBuilder.append(",\"orders\":").append(orders);
        stringBuilder.append(",\"tagFilters\":").append(tagFilters);
        stringBuilder.append(",\"dateInfo\":").append(dateInfo);
        stringBuilder.append(",\"params\":").append(params);
        stringBuilder.append(",\"limit\":").append(limit);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public List<String> getMetrics() {
        List<String> metrics = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(this.aggregators)) {
            metrics = aggregators.stream().map(Aggregator::getColumn).collect(Collectors.toList());
        }
        return metrics;
    }
}
