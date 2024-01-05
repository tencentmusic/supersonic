package com.tencent.supersonic.chat.core.query.rule.tag;

import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Order;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;

public abstract class TagListQuery extends TagSemanticQuery {

    @Override
    public void fillParseInfo(QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(queryContext, chatContext);
        this.addEntityDetailAndOrderByMetric(queryContext, parseInfo);
    }

    private void addEntityDetailAndOrderByMetric(QueryContext queryContext, SemanticParseInfo parseInfo) {
        Long modelId = parseInfo.getModelId();
        if (Objects.nonNull(modelId) && modelId > 0L) {
            ChatConfigRichResp chaConfigRichDesc = queryContext.getModelIdToChatRichConfig().get(modelId);
            ModelSchema modelSchema = queryContext.getSemanticSchema().getModelSchemaMap().get(parseInfo.getModelId());
            if (chaConfigRichDesc != null && chaConfigRichDesc.getChatDetailRichConfig() != null
                    && Objects.nonNull(modelSchema) && Objects.nonNull(modelSchema.getEntity())) {
                Set<SchemaElement> dimensions = new LinkedHashSet<>();
                Set<SchemaElement> metrics = new LinkedHashSet();
                Set<Order> orders = new LinkedHashSet();
                ChatDefaultRichConfigResp chatDefaultConfig = chaConfigRichDesc
                        .getChatDetailRichConfig().getChatDefaultConfig();
                if (chatDefaultConfig != null) {
                    if (CollectionUtils.isNotEmpty(chatDefaultConfig.getMetrics())) {
                        chatDefaultConfig.getMetrics().stream()
                                .forEach(metric -> {
                                    metrics.add(metric);
                                    orders.add(new Order(metric.getBizName(), Constants.DESC_UPPER));
                                });
                    }
                    if (CollectionUtils.isNotEmpty(chatDefaultConfig.getDimensions())) {
                        chatDefaultConfig.getDimensions().stream()
                                .forEach(dimension -> dimensions.add(dimension));
                    }
                }
                parseInfo.setDimensions(dimensions);
                parseInfo.setMetrics(metrics);
                parseInfo.setOrders(orders);
            }
        }
    }

}
