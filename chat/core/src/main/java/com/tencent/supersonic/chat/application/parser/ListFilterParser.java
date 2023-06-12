package com.tencent.supersonic.chat.application.parser;

import static com.tencent.supersonic.common.constant.Constants.DAY;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticParser;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.chat.application.query.EntityListFilter;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.chat.domain.utils.DefaultSemanticInternalUtils;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class ListFilterParser implements SemanticParser {


    private DefaultSemanticInternalUtils defaultSemanticUtils;

    @Override
    public boolean parse(QueryContextReq queryContext, ChatContext chatCtx) {
        defaultSemanticUtils = ContextUtils.getBean(DefaultSemanticInternalUtils.class);

        String queryMode = queryContext.getParseInfo().getQueryMode();
        if (!EntityListFilter.QUERY_MODE.equals(queryMode)) {
            return false;
        }
        this.fillDateEntityFilter(queryContext.getParseInfo());
        this.addEntityDetailAndOrderByMetric(queryContext, chatCtx);
        this.dealNativeQuery(queryContext, true);
        return false;

    }

    private void fillDateEntityFilter(SemanticParseInfo semanticParseInfo) {
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
        dateInfo.setUnit(1);
        dateInfo.setPeriod(DAY);
        dateInfo.setText(String.format("近1天"));
        semanticParseInfo.setDateInfo(dateInfo);
    }

    private void addEntityDetailAndOrderByMetric(QueryContextReq searchCtx, ChatContext chatCtx) {
        if (searchCtx.getParseInfo().getDomainId() > 0L) {
            ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(
                    searchCtx.getParseInfo().getDomainId());
            if (chaConfigRichDesc != null) {
                SemanticParseInfo semanticParseInfo = searchCtx.getParseInfo();
                List<SchemaItem> dimensions = new ArrayList();
                Set<String> primaryDimensions = this.addPrimaryDimension(chaConfigRichDesc.getEntity(), dimensions);
                List<SchemaItem> metrics = new ArrayList();
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
                List<Order> orders = new ArrayList();
                if (chaConfigRichDesc.getEntity() != null
                        && chaConfigRichDesc.getEntity().getEntityInternalDetailDesc() != null) {
                    chaConfigRichDesc.getEntity().getEntityInternalDetailDesc().getMetricList().stream()
                            .forEach((metric) -> orders.add(new Order(metric.getBizName(), Constants.DESC_UPPER)));
                }

                semanticParseInfo.setOrders(orders);
            }
        }

    }

    private Set<String> addPrimaryDimension(EntityRichInfo entity, List<SchemaItem> dimensions) {
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

    private void dealNativeQuery(QueryContextReq searchCtx, boolean isNativeQuery) {
        if (Objects.nonNull(searchCtx) && Objects.nonNull(searchCtx.getParseInfo())) {
            searchCtx.getParseInfo().setNativeQuery(isNativeQuery);
        }

    }
}