package com.tencent.supersonic.chat.query.rule.entity;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.config.ChatConfigRich;
import com.tencent.supersonic.chat.config.ChatDefaultRichConfig;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.util.ContextUtils;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class EntityListQuery extends EntitySemanticQuery{

    @Override
    public void fillParseInfo(Long domainId, ChatContext chatContext){
        super.fillParseInfo(domainId, chatContext);
        this.addEntityDetailAndOrderByMetric(parseInfo);
    }

    private void addEntityDetailAndOrderByMetric(SemanticParseInfo parseInfo) {
        if (parseInfo.getDomainId() > 0L) {
            ConfigService configService = ContextUtils.getBean(ConfigService.class);
            ChatConfigRich chaConfigRichDesc = configService.getConfigRichInfo(
                    parseInfo.getDomainId());
            if (chaConfigRichDesc != null && chaConfigRichDesc.getChatDetailRichConfig() != null
                    && chaConfigRichDesc.getChatDetailRichConfig().getEntity() != null) {
                Set<SchemaElement> dimensions = new LinkedHashSet();
                Set<SchemaElement> metrics = new LinkedHashSet();
                Set<Order> orders = new LinkedHashSet();
                ChatDefaultRichConfig chatDefaultConfig = chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig();
                if (chatDefaultConfig != null) {
                    chatDefaultConfig.getMetrics().stream()
                            .forEach(metric -> {
                                metrics.add(metric);
                                orders.add(new Order(metric.getBizName(), Constants.DESC_UPPER));
                            });
                    chatDefaultConfig.getDimensions().stream()
                            .forEach(dimension -> dimensions.add(dimension));

                }

                parseInfo.setDimensions(dimensions);
                parseInfo.setMetrics(metrics);
                parseInfo.setOrders(orders);
            }
        }
    }

}
