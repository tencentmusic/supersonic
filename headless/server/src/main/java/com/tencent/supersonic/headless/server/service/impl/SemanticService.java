package com.tencent.supersonic.headless.server.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.DataInfo;
import com.tencent.supersonic.headless.api.pojo.DataSetInfo;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.utils.QueryReqBuilder;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.service.SchemaService;
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

    @Autowired
    private QueryService queryService;

    public SemanticSchema getSemanticSchema() {
        return new SemanticSchema(schemaService.getDataSetSchema());
    }

    public DataSetSchema getDataSetSchema(Long id) {
        return schemaService.getDataSetSchema(id);
    }

    public EntityInfo getEntityInfo(SemanticParseInfo parseInfo, DataSetSchema dataSetSchema, User user) {
        if (parseInfo != null && parseInfo.getDataSetId() != null && parseInfo.getDataSetId() > 0) {
            EntityInfo entityInfo = getEntityBasicInfo(dataSetSchema);
            if (parseInfo.getDimensionFilters().size() <= 0 || entityInfo.getDataSetInfo() == null) {
                entityInfo.setMetrics(null);
                entityInfo.setDimensions(null);
                return entityInfo;
            }
            String primaryKey = entityInfo.getDataSetInfo().getPrimaryKey();
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
                    fillEntityInfoValue(entityInfo, dataSetSchema, user);
                    return entityInfo;
                } catch (Exception e) {
                    log.error("setMainModel error", e);
                }
            }
        }
        return null;
    }

    private EntityInfo getEntityBasicInfo(DataSetSchema dataSetSchema) {

        EntityInfo entityInfo = new EntityInfo();
        if (dataSetSchema == null) {
            return entityInfo;
        }
        Long dataSetId = dataSetSchema.getDataSet().getDataSet();
        DataSetInfo dataSetInfo = new DataSetInfo();
        dataSetInfo.setItemId(dataSetId.intValue());
        dataSetInfo.setName(dataSetSchema.getDataSet().getName());
        dataSetInfo.setWords(dataSetSchema.getDataSet().getAlias());
        dataSetInfo.setBizName(dataSetSchema.getDataSet().getBizName());
        if (Objects.nonNull(dataSetSchema.getEntity())) {
            dataSetInfo.setPrimaryKey(dataSetSchema.getEntity().getBizName());
        }
        entityInfo.setDataSetInfo(dataSetInfo);
        TagTypeDefaultConfig tagTypeDefaultConfig = dataSetSchema.getTagTypeDefaultConfig();
        if (tagTypeDefaultConfig == null || tagTypeDefaultConfig.getDefaultDisplayInfo() == null) {
            return entityInfo;
        }
        List<DataInfo> dimensions = tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds().stream()
                .map(id -> {
                    SchemaElement element = dataSetSchema.getElement(SchemaElementType.DIMENSION, id);
                    if (element == null) {
                        return null;
                    }
                    return new DataInfo(element.getId().intValue(), element.getName(), element.getBizName(), null);
                }).filter(Objects::nonNull).collect(Collectors.toList());
        List<DataInfo> metrics = tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds().stream()
                .map(id -> {
                    SchemaElement element = dataSetSchema.getElement(SchemaElementType.METRIC, id);
                    if (element == null) {
                        return null;
                    }
                    return new DataInfo(element.getId().intValue(), element.getName(), element.getBizName(), null);
                }).filter(Objects::nonNull).collect(Collectors.toList());
        entityInfo.setDimensions(dimensions);
        entityInfo.setMetrics(metrics);
        return entityInfo;
    }

    public void fillEntityInfoValue(EntityInfo entityInfo, DataSetSchema dataSetSchema, User user) {
        SemanticQueryResp queryResultWithColumns =
                getQueryResultWithSchemaResp(entityInfo, dataSetSchema, user);
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

    public SemanticQueryResp getQueryResultWithSchemaResp(EntityInfo entityInfo,
                                                          DataSetSchema dataSetSchema, User user) {
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setDataSet(dataSetSchema.getDataSet());
        semanticParseInfo.setQueryType(QueryType.DETAIL);
        semanticParseInfo.setMetrics(getMetrics(entityInfo));
        semanticParseInfo.setDimensions(getDimensions(entityInfo));
        DateConf dateInfo = new DateConf();
        int unit = 1;
        TimeDefaultConfig timeDefaultConfig = dataSetSchema.getTagTypeTimeDefaultConfig();
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

        //add filter
        QueryFilter chatFilter = getQueryFilter(entityInfo);
        Set<QueryFilter> chatFilters = new LinkedHashSet();
        chatFilters.add(chatFilter);
        semanticParseInfo.setDimensionFilters(chatFilters);

        SemanticQueryResp queryResultWithColumns = null;
        try {
            QueryStructReq queryStructReq = QueryReqBuilder.buildStructReq(semanticParseInfo);
            queryResultWithColumns = queryService.queryByReq(queryStructReq, user);
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
        return entityInfo.getDataSetInfo().getPrimaryKey();
    }

}
