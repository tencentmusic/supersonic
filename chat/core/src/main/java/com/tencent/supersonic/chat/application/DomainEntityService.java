package com.tencent.supersonic.chat.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.DataInfo;
import com.tencent.supersonic.chat.api.pojo.DomainInfo;
import com.tencent.supersonic.chat.api.pojo.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.chat.domain.utils.DefaultSemanticInternalUtils;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DomainEntityService {

    private final Logger logger = LoggerFactory.getLogger(DomainEntityService.class);
    @Autowired
    private SemanticLayer semanticLayer;

    @Autowired
    private DefaultSemanticInternalUtils defaultSemanticUtils;

    public EntityInfo getEntityInfo(QueryContextReq queryCtx, ChatContext chatCtx, User user) {
        SemanticParseInfo parseInfo = queryCtx.getParseInfo();

        if (parseInfo != null && parseInfo.getDomainId() > 0) {
            EntityInfo entityInfo = getEntityInfo(parseInfo.getDomainId());
            if (parseInfo.getDimensionFilters().size() <= 0) {
                entityInfo.setMetrics(null);
                entityInfo.setDimensions(null);
                return entityInfo;
            }
            if (entityInfo.getDomainInfo() != null && entityInfo.getDomainInfo().getPrimaryEntityBizName() != null) {
                String domainInfoPrimaryName = entityInfo.getDomainInfo().getPrimaryEntityBizName();
                String domainInfoId = "";
                for (Filter chatFilter : parseInfo.getDimensionFilters()) {
                    if (chatFilter.getBizName().equals(domainInfoPrimaryName)) {
                        if (chatFilter.getOperator().equals(FilterOperatorEnum.EQUALS)) {
                            domainInfoId = chatFilter.getValue().toString();
                        }
                        if (chatFilter.getOperator().equals(FilterOperatorEnum.IN)) {
                            domainInfoId = ((List<String>) chatFilter.getValue()).get(0);
                        }
                    }
                }
                if (!"".equals(domainInfoId)) {
                    try {
                        setMainDomain(entityInfo, parseInfo.getDomainId(),
                                domainInfoId, user);

                        return entityInfo;
                    } catch (Exception e) {
                        logger.error("setMaintDomain error {}", e);
                    }
                }
            }
        }
        return null;
    }

    public EntityInfo getEntityInfo(Long domain) {
        ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(domain);
        return getEntityInfo(chaConfigRichDesc.getEntity());
    }

    private EntityInfo getEntityInfo(EntityRichInfo entityDesc) {
        EntityInfo entityInfo = new EntityInfo();

        if (entityDesc != null) {
            DomainInfo domainInfo = new DomainInfo();
            domainInfo.setItemId(Integer.valueOf(entityDesc.getDomainId().intValue()));
            domainInfo.setName(entityDesc.getDomainName());
            domainInfo.setWords(entityDesc.getNames());
            domainInfo.setBizName(entityDesc.getDomainBizName());
            if (entityDesc.getEntityIds().size() > 0) {
                domainInfo.setPrimaryEntityBizName(entityDesc.getEntityIds().get(0).getBizName());
            }
            entityInfo.setDomainInfo(domainInfo);
            List<DataInfo> dimensions = new ArrayList<>();
            List<DataInfo> metrics = new ArrayList<>();
            if (entityDesc.getEntityInternalDetailDesc() != null) {
                for (DimSchemaResp dimensionDesc : entityDesc.getEntityInternalDetailDesc().getDimensionList()) {
                    DataInfo mainEntityDimension = new DataInfo();
                    mainEntityDimension.setItemId(dimensionDesc.getId().intValue());
                    mainEntityDimension.setName(dimensionDesc.getName());
                    mainEntityDimension.setBizName(dimensionDesc.getBizName());
                    dimensions.add(mainEntityDimension);
                }
                entityInfo.setDimensions(dimensions);
                for (MetricSchemaResp metricDesc : entityDesc.getEntityInternalDetailDesc().getMetricList()) {
                    DataInfo dataInfo = new DataInfo();
                    dataInfo.setName(metricDesc.getName());
                    dataInfo.setBizName(metricDesc.getBizName());
                    dataInfo.setItemId(metricDesc.getId().intValue());
                    metrics.add(dataInfo);
                }
                entityInfo.setMetrics(metrics);
            }
        }
        return entityInfo;
    }

    public void setMainDomain(EntityInfo domainInfo, Long domain, String entity, User user) {
        domainInfo.setEntityId(entity);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setDomainId(Long.valueOf(domain));
        semanticParseInfo.setNativeQuery(true);
        semanticParseInfo.setMetrics(getMetrics(domainInfo));
        semanticParseInfo.setDimensions(getDimensions(domainInfo));
        DateConf dateInfo = new DateConf();
        dateInfo.setUnit(1);
        dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
        semanticParseInfo.setDateInfo(dateInfo);

        // add filter
        Filter chatFilter = new Filter();
        chatFilter.setValue(String.valueOf(entity));
        chatFilter.setOperator(FilterOperatorEnum.EQUALS);
        chatFilter.setBizName(getEntityPrimaryName(domainInfo));
        List<Filter> chatFilters = new ArrayList<>();
        chatFilters.add(chatFilter);
        semanticParseInfo.setDimensionFilters(chatFilters);

        QueryResultWithSchemaResp queryResultWithColumns = null;
        try {
            queryResultWithColumns = semanticLayer.queryByStruct(SchemaInfoConverter.convertTo(semanticParseInfo),
                    user);
        } catch (Exception e) {
            logger.warn("setMainDomain queryByStruct error, e:", e);
        }

        if (queryResultWithColumns != null) {
            if (!CollectionUtils.isEmpty(queryResultWithColumns.getResultList())
                    && queryResultWithColumns.getResultList().size() > 0) {
                Map<String, Object> result = queryResultWithColumns.getResultList().get(0);
                for (Map.Entry<String, Object> entry : result.entrySet()) {
                    String entryKey = getEntryKey(entry);
                    if (entry.getValue() == null || entryKey == null) {
                        continue;
                    }
                    domainInfo.getDimensions().stream().filter(i -> entryKey.equals(i.getBizName()))
                            .forEach(i -> i.setValue(entry.getValue().toString()));
                    domainInfo.getMetrics().stream().filter(i -> entryKey.equals(i.getBizName()))
                            .forEach(i -> i.setValue(entry.getValue().toString()));
                }
            }
        }
    }

    private List<SchemaItem> getDimensions(EntityInfo domainInfo) {
        List<SchemaItem> dimensions = new ArrayList<>();
        for (DataInfo mainEntityDimension : domainInfo.getDimensions()) {
            SchemaItem dimension = new SchemaItem();
            dimension.setBizName(mainEntityDimension.getBizName());
            dimensions.add(dimension);
        }
        return dimensions;
    }

    private String getEntryKey(Map.Entry<String, Object> entry) {
        // metric parser special handle, TODO delete
        String entryKey = entry.getKey();
        if (entryKey.contains("__")) {
            entryKey = entryKey.split("__")[1];
        }
        return entryKey;
    }

    private List<SchemaItem> getMetrics(EntityInfo domainInfo) {
        List<SchemaItem> metrics = new ArrayList<>();
        for (DataInfo metricValue : domainInfo.getMetrics()) {
            SchemaItem metric = new SchemaItem();
            metric.setBizName(metricValue.getBizName());
            metrics.add(metric);
        }
        return metrics;
    }

    private String getEntityPrimaryName(EntityInfo domainInfo) {
        return domainInfo.getDomainInfo().getPrimaryEntityBizName();
    }

}
