package com.tencent.supersonic.chat.application.query;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.REQUIRED;
import static com.tencent.supersonic.common.constant.Constants.DAY;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import com.tencent.supersonic.chat.domain.utils.DefaultSemanticInternalUtils;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class EntityListFilter extends EntitySemanticQuery {

    public static String QUERY_MODE = "ENTITY_LIST_FILTER";
    private static Long entityListLimit = 200L;

    public EntityListFilter() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 1);
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
            DefaultSemanticInternalUtils defaultSemanticUtils = ContextUtils.getBean(
                    DefaultSemanticInternalUtils.class);
            ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(
                    semanticParseInfo.getDomainId());
            if (chaConfigRichDesc != null) {
                //SemanticParseInfo semanticParseInfo = queryContext.getParseInfo();
                Set<SchemaItem> dimensions = new LinkedHashSet();
                Set<String> primaryDimensions = this.addPrimaryDimension(chaConfigRichDesc.getEntity(), dimensions);
                Set<SchemaItem> metrics = new LinkedHashSet();
                if (chaConfigRichDesc.getEntity() != null
                        && chaConfigRichDesc.getEntity().getEntityInternalDetailDesc() != null) {
                    chaConfigRichDesc.getEntity().getEntityInternalDetailDesc().getMetricList().stream()
                            .forEach((m) -> metrics.add(this.getMetric(m)));
                    chaConfigRichDesc.getEntity().getEntityInternalDetailDesc().getDimensionList().stream()
                            .filter((m) -> !primaryDimensions.contains(m.getBizName()))
                            .forEach((m) -> dimensions.add(this.getDimension(m)));
                }

                semanticParseInfo.setDimensions(dimensions);
                semanticParseInfo.setMetrics(metrics);
                Set<Order> orders = new LinkedHashSet();
                if (chaConfigRichDesc.getEntity() != null
                        && chaConfigRichDesc.getEntity().getEntityInternalDetailDesc() != null) {
                    chaConfigRichDesc.getEntity().getEntityInternalDetailDesc().getMetricList().stream()
                            .forEach((metric) -> orders.add(new Order(metric.getBizName(), Constants.DESC_UPPER)));
                }

                semanticParseInfo.setOrders(orders);
            }
        }

    }

    private Set<String> addPrimaryDimension(EntityRichInfo entity, Set<SchemaItem> dimensions) {
        Set<String> primaryDimensions = new HashSet();
        if (!Objects.isNull(entity) && !CollectionUtils.isEmpty(entity.getEntityIds())) {
            entity.getEntityIds().stream().forEach((dimSchemaDesc) -> {
                SchemaItem dimension = new SchemaItem();
                BeanUtils.copyProperties(dimSchemaDesc, dimension);
                dimensions.add(dimension);
                primaryDimensions.add(dimSchemaDesc.getBizName());
            });
            return primaryDimensions;
        } else {
            return primaryDimensions;
        }
    }

    private SchemaItem getMetric(MetricSchemaResp metricSchemaDesc) {
        SchemaItem queryMeta = new SchemaItem();
        queryMeta.setId(metricSchemaDesc.getId());
        queryMeta.setBizName(metricSchemaDesc.getBizName());
        queryMeta.setName(metricSchemaDesc.getName());
        return queryMeta;
    }

    private SchemaItem getDimension(DimSchemaResp dimSchemaDesc) {
        SchemaItem queryMeta = new SchemaItem();
        queryMeta.setId(dimSchemaDesc.getId());
        queryMeta.setBizName(dimSchemaDesc.getBizName());
        queryMeta.setName(dimSchemaDesc.getName());
        return queryMeta;
    }

    private void dealNativeQuery(SemanticParseInfo semanticParseInfo, boolean isNativeQuery) {
        if (Objects.nonNull(semanticParseInfo)) {
            semanticParseInfo.setNativeQuery(isNativeQuery);
        }

    }

}
