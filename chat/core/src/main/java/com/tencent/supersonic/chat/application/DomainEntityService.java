package com.tencent.supersonic.chat.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.DataInfo;
import com.tencent.supersonic.chat.api.pojo.DomainInfo;
import com.tencent.supersonic.chat.api.pojo.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatDefaultRichConfig;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.chat.domain.utils.ComponentFactory;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class DomainEntityService {

    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    @Autowired
    private ConfigServiceImpl configService;

    public EntityInfo getEntityInfo(SemanticParseInfo parseInfo, User user) {
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
                    if (chatFilter != null && chatFilter.getBizName() != null && chatFilter.getBizName()
                            .equals(domainInfoPrimaryName)) {
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
                        log.error("setMaintDomain error {}", e);
                    }
                }
            }
        }
        return null;
    }

    public EntityInfo getEntityInfo(Long domain) {
        ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(domain);
        if (Objects.isNull(chaConfigRichDesc) || Objects.isNull(chaConfigRichDesc.getChatDetailRichConfig())) {
            return new EntityInfo();
        }
        return getEntityInfo(chaConfigRichDesc);
    }

    private EntityInfo getEntityInfo(ChatConfigRichResp chaConfigRichDesc) {

        EntityInfo entityInfo = new EntityInfo();
        EntityRichInfo entityDesc = chaConfigRichDesc.getChatDetailRichConfig().getEntity();
        if (entityDesc != null && Objects.nonNull(chaConfigRichDesc.getDomainId())) {
            DomainInfo domainInfo = new DomainInfo();
            domainInfo.setItemId(Integer.valueOf(chaConfigRichDesc.getDomainId().intValue()));
            domainInfo.setName(chaConfigRichDesc.getDomainName());
            domainInfo.setWords(entityDesc.getNames());
            domainInfo.setBizName(chaConfigRichDesc.getBizName());
            if (Objects.nonNull(entityDesc.getDimItem())) {
                domainInfo.setPrimaryEntityBizName(entityDesc.getDimItem().getBizName());
            }

            entityInfo.setDomainInfo(domainInfo);
            List<DataInfo> dimensions = new ArrayList<>();
            List<DataInfo> metrics = new ArrayList<>();

            if (Objects.nonNull(chaConfigRichDesc) && Objects.nonNull(chaConfigRichDesc.getChatDetailRichConfig())
                    && Objects.nonNull(chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig())) {
                ChatDefaultRichConfig chatDefaultConfig = chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig();
                if(!CollectionUtils.isEmpty(chatDefaultConfig.getDimensions())){
                    for (SchemaItem dimensionDesc : chatDefaultConfig.getDimensions()) {
                        DataInfo mainEntityDimension = new DataInfo();
                        mainEntityDimension.setItemId(dimensionDesc.getId().intValue());
                        mainEntityDimension.setName(dimensionDesc.getName());
                        mainEntityDimension.setBizName(dimensionDesc.getBizName());
                        dimensions.add(mainEntityDimension);
                    }
                    entityInfo.setDimensions(dimensions);
                }

                if(!CollectionUtils.isEmpty(chatDefaultConfig.getMetrics())){
                    for (SchemaItem metricDesc : chatDefaultConfig.getMetrics()) {
                        DataInfo dataInfo = new DataInfo();
                        dataInfo.setName(metricDesc.getName());
                        dataInfo.setBizName(metricDesc.getBizName());
                        dataInfo.setItemId(metricDesc.getId().intValue());
                        metrics.add(dataInfo);
                    }
                    entityInfo.setMetrics(metrics);
                }
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
        Set<Filter> chatFilters = new LinkedHashSet();
        chatFilters.add(chatFilter);
        semanticParseInfo.setDimensionFilters(chatFilters);

        QueryResultWithSchemaResp queryResultWithColumns = null;
        try {
            queryResultWithColumns = semanticLayer.queryByStruct(SchemaInfoConverter.convertTo(semanticParseInfo),
                    user);
        } catch (Exception e) {
            log.warn("setMainDomain queryByStruct error, e:", e);
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

    private Set<SchemaItem> getDimensions(EntityInfo domainInfo) {
        Set<SchemaItem> dimensions = new LinkedHashSet();
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

    private Set<SchemaItem> getMetrics(EntityInfo domainInfo) {
        Set<SchemaItem> metrics = new LinkedHashSet();
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
