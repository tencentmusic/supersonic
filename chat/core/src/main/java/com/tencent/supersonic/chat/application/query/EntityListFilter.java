package com.tencent.supersonic.chat.application.query;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.*;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.*;
import static com.tencent.supersonic.common.constant.Constants.DAY;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatDefaultRichConfig;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.chat.domain.service.ConfigService;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class EntityListFilter extends EntitySemanticQuery {

    public static String QUERY_MODE = "ENTITY_LIST_FILTER";
    private static Long entityListLimit = 200L;


    public EntityListFilter() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 1)
                .addOption(ENTITY, REQUIRED, AT_LEAST, 1);
    }


    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }


    @Override
    public void inheritContext(ChatContext chatContext) {
        SemanticParseInfo chatParseInfo = chatContext.getParseInfo();
        ContextHelper.addIfEmpty(chatParseInfo.getDimensionFilters(), parseInfo.getDimensionFilters());
        parseInfo.setLimit(entityListLimit);
        this.fillDateEntityFilter(parseInfo);
        this.addEntityDetailAndOrderByMetric(parseInfo);
        this.dealNativeQuery(parseInfo, true);
    }


    private void fillDateEntityFilter(SemanticParseInfo semanticParseInfo) {
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
        dateInfo.setUnit(1);
        dateInfo.setPeriod(DAY);
        dateInfo.setText(String.format("近1天"));
        semanticParseInfo.setDateInfo(dateInfo);
    }

    private void addEntityDetailAndOrderByMetric(SemanticParseInfo semanticParseInfo) {
        if (semanticParseInfo.getDomainId() > 0L) {
            ConfigService configService = ContextUtils.getBean(ConfigService.class);
            ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(
                    semanticParseInfo.getDomainId());
            if (chaConfigRichDesc != null && chaConfigRichDesc.getChatDetailRichConfig() != null
                    && chaConfigRichDesc.getChatDetailRichConfig().getEntity() != null) {
//                SemanticParseInfo semanticParseInfo = queryContext.getParseInfo();
//                EntityRichInfo entity = chaConfigRichDesc.getChatDetailRichConfig().getEntity();
                Set<SchemaItem> dimensions = new LinkedHashSet();
//                Set<String> primaryDimensions = this.addPrimaryDimension(entity, dimensions);
                Set<SchemaItem> metrics = new LinkedHashSet();
                Set<Order> orders = new LinkedHashSet();
                ChatDefaultRichConfig chatDefaultConfig = chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig();
                if (chatDefaultConfig != null) {
                    chatDefaultConfig.getMetrics().stream()
                            .forEach(metric -> {
                                metrics.add(metric);
                                orders.add(new Order(metric.getBizName(), Constants.DESC_UPPER));
                            });
                    chatDefaultConfig.getDimensions().stream()
//                            .filter((m) -> !primaryDimensions.contains(m.getBizName()))
                            .forEach(dimension -> dimensions.add(dimension));

                }

                semanticParseInfo.setDimensions(dimensions);
                semanticParseInfo.setMetrics(metrics);
                semanticParseInfo.setOrders(orders);
            }
        }

    }

    private Set<String> addPrimaryDimension(EntityRichInfo entity, Set<SchemaItem> dimensions) {
        Set<String> primaryDimensions = new HashSet();
        DimSchemaResp dimItem = entity.getDimItem();
        if (Objects.nonNull(entity) && Objects.nonNull(dimItem)) {
            SchemaItem dimension = new SchemaItem();
            BeanUtils.copyProperties(dimItem, dimension);
            dimensions.add(dimension);
            primaryDimensions.add(dimItem.getBizName());
            return primaryDimensions;
        } else {
            return primaryDimensions;
        }
    }

    private void dealNativeQuery(SemanticParseInfo semanticParseInfo, boolean isNativeQuery) {
        if (Objects.nonNull(semanticParseInfo)) {
            semanticParseInfo.setNativeQuery(isNativeQuery);
        }

    }

}
