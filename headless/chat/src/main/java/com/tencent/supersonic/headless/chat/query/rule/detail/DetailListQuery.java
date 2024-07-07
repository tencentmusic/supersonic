package com.tencent.supersonic.headless.chat.query.rule.detail;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.QueryContext;
import org.apache.commons.collections.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class DetailListQuery extends DetailSemanticQuery {

    @Override
    public void fillParseInfo(QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(queryContext, chatContext);
        this.addEntityDetailAndOrderByMetric(queryContext, parseInfo);
    }

    private void addEntityDetailAndOrderByMetric(QueryContext queryContext, SemanticParseInfo parseInfo) {
        Long dataSetId = parseInfo.getDataSetId();
        if (Objects.nonNull(dataSetId) && dataSetId > 0L) {
            DataSetSchema dataSetSchema = queryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
            if (dataSetSchema != null && Objects.nonNull(dataSetSchema.getEntity())) {
                Set<SchemaElement> dimensions = new LinkedHashSet<>();
                Set<SchemaElement> metrics = new LinkedHashSet<>();
                Set<Order> orders = new LinkedHashSet<>();
                TagTypeDefaultConfig tagTypeDefaultConfig = dataSetSchema.getTagTypeDefaultConfig();
                if (tagTypeDefaultConfig != null && tagTypeDefaultConfig.getDefaultDisplayInfo() != null) {
                    if (CollectionUtils.isNotEmpty(tagTypeDefaultConfig.getDefaultDisplayInfo().getMetricIds())) {
                        metrics = tagTypeDefaultConfig.getDefaultDisplayInfo().getMetricIds()
                                .stream().map(id -> {
                                    SchemaElement metric = dataSetSchema.getElement(SchemaElementType.METRIC, id);
                                    if (metric != null) {
                                        orders.add(new Order(metric.getBizName(), Constants.DESC_UPPER));
                                    }
                                    return metric;
                                }).filter(Objects::nonNull).collect(Collectors.toSet());
                    }
                    if (CollectionUtils.isNotEmpty(tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds())) {
                        dimensions = tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds().stream()
                                .map(id -> dataSetSchema.getElement(SchemaElementType.DIMENSION, id))
                                .filter(Objects::nonNull).collect(Collectors.toSet());
                    }
                }
                parseInfo.setDimensions(dimensions);
                parseInfo.setMetrics(metrics);
                parseInfo.setOrders(orders);
            }
        }
    }

}
