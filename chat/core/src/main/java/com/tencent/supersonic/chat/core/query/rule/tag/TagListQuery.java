package com.tencent.supersonic.chat.core.query.rule.tag;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import org.apache.commons.collections.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TagListQuery extends TagSemanticQuery {

    @Override
    public void fillParseInfo(QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(queryContext, chatContext);
        this.addEntityDetailAndOrderByMetric(queryContext, parseInfo);
    }

    private void addEntityDetailAndOrderByMetric(QueryContext queryContext, SemanticParseInfo parseInfo) {
        Long viewId = parseInfo.getViewId();
        if (Objects.nonNull(viewId) && viewId > 0L) {
            ViewSchema viewSchema = queryContext.getSemanticSchema().getViewSchemaMap().get(viewId);
            if (viewSchema != null && Objects.nonNull(viewSchema.getEntity())) {
                Set<SchemaElement> dimensions = new LinkedHashSet<>();
                Set<SchemaElement> metrics = new LinkedHashSet<>();
                Set<Order> orders = new LinkedHashSet<>();
                TagTypeDefaultConfig tagTypeDefaultConfig = viewSchema.getTagTypeDefaultConfig();
                if (tagTypeDefaultConfig != null && tagTypeDefaultConfig.getDefaultDisplayInfo() != null) {
                    if (CollectionUtils.isNotEmpty(tagTypeDefaultConfig.getDefaultDisplayInfo().getMetricIds())) {
                        metrics = tagTypeDefaultConfig.getDefaultDisplayInfo().getMetricIds()
                                .stream().map(id -> {
                                    SchemaElement metric = viewSchema.getElement(SchemaElementType.METRIC, id);
                                    if (metric != null) {
                                        orders.add(new Order(metric.getBizName(), Constants.DESC_UPPER));
                                    }
                                    return metric;
                                }).filter(Objects::nonNull).collect(Collectors.toSet());
                    }
                    if (CollectionUtils.isNotEmpty(tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds())) {
                        dimensions = tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds().stream()
                                .map(id -> viewSchema.getElement(SchemaElementType.DIMENSION, id))
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
