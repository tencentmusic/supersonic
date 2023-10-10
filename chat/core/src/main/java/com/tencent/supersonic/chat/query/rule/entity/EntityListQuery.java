package com.tencent.supersonic.chat.query.rule.entity;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;

public abstract class EntityListQuery extends EntitySemanticQuery {

    @Override
    public void fillParseInfo(Long modelId, QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(modelId, queryContext, chatContext);
        this.addEntityDetailAndOrderByMetric(parseInfo);
    }

    private void addEntityDetailAndOrderByMetric(SemanticParseInfo parseInfo) {
        Long modelId = parseInfo.getModelId();
        if (Objects.nonNull(modelId) && modelId > 0L) {
            ConfigService configService = ContextUtils.getBean(ConfigService.class);
            ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(parseInfo.getModelId());
            SemanticService schemaService = ContextUtils.getBean(SemanticService.class);
            ModelSchema modelSchema = schemaService.getModelSchema(modelId);

            if (chaConfigRichDesc != null && chaConfigRichDesc.getChatDetailRichConfig() != null
                    && Objects.nonNull(modelSchema) && Objects.nonNull(modelSchema.getEntity())) {
                Set<SchemaElement> dimensions = new LinkedHashSet();
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
