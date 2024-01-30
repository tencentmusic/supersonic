package com.tencent.supersonic.chat.server.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.DataInfo;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ViewInfo;
import com.tencent.supersonic.chat.core.knowledge.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.chat.core.utils.QueryReqBuilder;
import com.tencent.supersonic.chat.server.service.impl.SchemaService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SemanticService {

    @Autowired
    private SchemaService schemaService;

    private SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();

    public SemanticSchema getSemanticSchema() {
        return schemaService.getSemanticSchema();
    }

    public ViewSchema getModelSchema(Long id) {
        return schemaService.getViewSchema(id);
    }

    public EntityInfo getEntityInfo(SemanticParseInfo parseInfo, ViewSchema viewSchema, User user) {
        if (parseInfo != null && parseInfo.getViewId() > 0) {
            EntityInfo entityInfo = getEntityBasicInfo(viewSchema);
            if (parseInfo.getDimensionFilters().size() <= 0 || entityInfo.getViewInfo() == null) {
                entityInfo.setMetrics(null);
                entityInfo.setDimensions(null);
                return entityInfo;
            }
            String primaryKey = entityInfo.getViewInfo().getPrimaryKey();
            if (StringUtils.isNotBlank(primaryKey)) {
                String entityId = "";
                for (QueryFilter chatFilter : parseInfo.getDimensionFilters()) {
                    if (chatFilter != null && chatFilter.getBizName() != null && chatFilter.getBizName()
                            .equals(primaryKey)) {
                        if (chatFilter.getOperator().equals(FilterOperatorEnum.EQUALS)) {
                            entityId = chatFilter.getValue().toString();
                        }
                    }
                }
                entityInfo.setEntityId(entityId);
                try {
                    fillEntityInfoValue(entityInfo, viewSchema, user);
                    return entityInfo;
                } catch (Exception e) {
                    log.error("setMainModel error", e);
                }
            }
        }
        return null;
    }

    private EntityInfo getEntityBasicInfo(ViewSchema viewSchema) {

        EntityInfo entityInfo = new EntityInfo();
        if (viewSchema == null) {
            return entityInfo;
        }
        Long viewId = viewSchema.getView().getView();
        ViewInfo viewInfo = new ViewInfo();
        viewInfo.setItemId(viewId.intValue());
        viewInfo.setName(viewSchema.getView().getName());
        viewInfo.setWords(viewSchema.getView().getAlias());
        viewInfo.setBizName(viewSchema.getView().getBizName());
        if (Objects.nonNull(viewSchema.getEntity())) {
            viewInfo.setPrimaryKey(viewSchema.getEntity().getBizName());
        }
        entityInfo.setViewInfo(viewInfo);
        TagTypeDefaultConfig tagTypeDefaultConfig = viewSchema.getTagTypeDefaultConfig();
        if (tagTypeDefaultConfig == null) {
            return entityInfo;
        }
        List<DataInfo> dimensions = tagTypeDefaultConfig.getDimensionIds().stream()
                .map(id -> {
                    SchemaElement element = viewSchema.getElement(SchemaElementType.DIMENSION, id);
                    if (element == null) {
                        return null;
                    }
                    return new DataInfo(element.getId().intValue(), element.getName(), element.getBizName(), null);
                }).filter(Objects::nonNull).collect(Collectors.toList());
        List<DataInfo> metrics = tagTypeDefaultConfig.getDimensionIds().stream()
                .map(id -> {
                    SchemaElement element = viewSchema.getElement(SchemaElementType.METRIC, id);
                    if (element == null) {
                        return null;
                    }
                    return new DataInfo(element.getId().intValue(), element.getName(), element.getBizName(), null);
                }).filter(Objects::nonNull).collect(Collectors.toList());
        entityInfo.setDimensions(dimensions);
        entityInfo.setMetrics(metrics);
        return entityInfo;
    }

    public void fillEntityInfoValue(EntityInfo entityInfo, ViewSchema viewSchema, User user) {
        SemanticQueryResp queryResultWithColumns =
                getQueryResultWithSchemaResp(entityInfo, viewSchema, user);
        if (queryResultWithColumns != null) {
            if (!CollectionUtils.isEmpty(queryResultWithColumns.getResultList())
                    && queryResultWithColumns.getResultList().size() > 0) {
                Map<String, Object> result = queryResultWithColumns.getResultList().get(0);
                for (Map.Entry<String, Object> entry : result.entrySet()) {
                    String entryKey = getEntryKey(entry);
                    if (entry.getValue() == null || entryKey == null) {
                        continue;
                    }
                    entityInfo.getDimensions().stream().filter(i -> entryKey.equals(i.getBizName()))
                            .forEach(i -> i.setValue(entry.getValue().toString()));
                    entityInfo.getMetrics().stream().filter(i -> entryKey.equals(i.getBizName()))
                            .forEach(i -> i.setValue(entry.getValue().toString()));
                }
            }
        }
    }

    public SemanticQueryResp getQueryResultWithSchemaResp(EntityInfo entityInfo, ViewSchema viewSchema, User user) {
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setView(viewSchema.getView());
        semanticParseInfo.setQueryType(QueryType.TAG);
        semanticParseInfo.setMetrics(getMetrics(entityInfo));
        semanticParseInfo.setDimensions(getDimensions(entityInfo));
        DateConf dateInfo = new DateConf();
        int unit = 1;
        TimeDefaultConfig timeDefaultConfig = viewSchema.getTagTypeTimeDefaultConfig();
        if (Objects.nonNull(timeDefaultConfig)) {
            unit = timeDefaultConfig.getUnit();
            String date = LocalDate.now().plusDays(-unit).toString();
            dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
            dateInfo.setStartDate(date);
            dateInfo.setEndDate(date);
        } else {
            dateInfo.setUnit(unit);
            dateInfo.setDateMode(DateConf.DateMode.RECENT);
        }
        semanticParseInfo.setDateInfo(dateInfo);

        // add filter
        QueryFilter chatFilter = getQueryFilter(entityInfo);
        Set<QueryFilter> chatFilters = new LinkedHashSet();
        chatFilters.add(chatFilter);
        semanticParseInfo.setDimensionFilters(chatFilters);

        SemanticQueryResp queryResultWithColumns = null;
        try {
            QueryStructReq queryStructReq = QueryReqBuilder.buildStructReq(semanticParseInfo);
            queryResultWithColumns = semanticInterpreter.queryByStruct(queryStructReq, user);
        } catch (Exception e) {
            log.warn("setMainModel queryByStruct error, e:", e);
        }
        return queryResultWithColumns;
    }

    private QueryFilter getQueryFilter(EntityInfo entityInfo) {
        QueryFilter chatFilter = new QueryFilter();
        chatFilter.setValue(entityInfo.getEntityId());
        chatFilter.setOperator(FilterOperatorEnum.EQUALS);
        chatFilter.setBizName(getEntityPrimaryName(entityInfo));
        return chatFilter;
    }

    private Set<SchemaElement> getDimensions(EntityInfo modelInfo) {
        Set<SchemaElement> dimensions = new LinkedHashSet();
        for (DataInfo mainEntityDimension : modelInfo.getDimensions()) {
            SchemaElement dimension = new SchemaElement();
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

    private Set<SchemaElement> getMetrics(EntityInfo modelInfo) {
        Set<SchemaElement> metrics = new LinkedHashSet();
        for (DataInfo metricValue : modelInfo.getMetrics()) {
            SchemaElement metric = new SchemaElement();
            BeanUtils.copyProperties(metricValue, metric);
            metrics.add(metric);
        }
        return metrics;
    }

    private String getEntityPrimaryName(EntityInfo entityInfo) {
        return entityInfo.getViewInfo().getPrimaryKey();
    }

}
