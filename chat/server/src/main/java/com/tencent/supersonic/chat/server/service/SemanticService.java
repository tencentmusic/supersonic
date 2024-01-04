package com.tencent.supersonic.chat.server.service;


import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.ChatDefaultConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.DataInfo;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ModelInfo;
import com.tencent.supersonic.chat.core.config.AggregatorConfig;
import com.tencent.supersonic.chat.core.knowledge.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.chat.core.utils.QueryReqBuilder;
import com.tencent.supersonic.chat.server.service.impl.SchemaService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.ModelCluster;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class SemanticService {

    @Autowired
    private SchemaService schemaService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private AggregatorConfig aggregatorConfig;

    private SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();

    public SemanticSchema getSemanticSchema() {
        return schemaService.getSemanticSchema();
    }

    public ModelSchema getModelSchema(Long id) {
        return schemaService.getModelSchema(id);
    }

    public EntityInfo getEntityInfo(SemanticParseInfo parseInfo, User user) {
        if (parseInfo != null && parseInfo.getModelId() > 0) {
            EntityInfo entityInfo = getEntityInfo(parseInfo.getModelId());
            if (parseInfo.getDimensionFilters().size() <= 0 || entityInfo.getModelInfo() == null) {
                entityInfo.setMetrics(null);
                entityInfo.setDimensions(null);
                return entityInfo;
            }
            String primaryKey = entityInfo.getModelInfo().getPrimaryKey();
            if (StringUtils.isNotBlank(primaryKey)) {
                String modelInfoId = "";
                for (QueryFilter chatFilter : parseInfo.getDimensionFilters()) {
                    if (chatFilter != null && chatFilter.getBizName() != null && chatFilter.getBizName()
                            .equals(primaryKey)) {
                        if (chatFilter.getOperator().equals(FilterOperatorEnum.EQUALS)) {
                            modelInfoId = chatFilter.getValue().toString();
                        }
                    }
                }
                try {
                    setMainModel(entityInfo, parseInfo, modelInfoId, user);
                    return entityInfo;
                } catch (Exception e) {
                    log.error("setMainModel error", e);
                }
            }
        }
        return null;
    }

    public EntityInfo getEntityInfo(Long model) {
        ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(model);
        if (Objects.isNull(chaConfigRichDesc) || Objects.isNull(chaConfigRichDesc.getChatDetailRichConfig())) {
            return new EntityInfo();
        }
        return getEntityInfo(chaConfigRichDesc);
    }

    private EntityInfo getEntityInfo(ChatConfigRichResp chaConfigRichDesc) {

        EntityInfo entityInfo = new EntityInfo();
        Long modelId = chaConfigRichDesc.getModelId();
        if (Objects.nonNull(chaConfigRichDesc) && Objects.nonNull(modelId)) {
            SemanticService schemaService = ContextUtils.getBean(SemanticService.class);
            ModelSchema modelSchema = schemaService.getModelSchema(modelId);
            if (Objects.isNull(modelSchema) || Objects.isNull(modelSchema.getEntity())) {
                return entityInfo;
            }
            ModelInfo modelInfo = new ModelInfo();
            modelInfo.setItemId(modelId.intValue());
            modelInfo.setName(modelSchema.getModel().getName());
            modelInfo.setWords(modelSchema.getModel().getAlias());
            modelInfo.setBizName(modelSchema.getModel().getBizName());
            if (Objects.nonNull(modelSchema.getEntity())) {
                modelInfo.setPrimaryKey(modelSchema.getEntity().getBizName());
            }

            entityInfo.setModelInfo(modelInfo);
            List<DataInfo> dimensions = new ArrayList<>();
            List<DataInfo> metrics = new ArrayList<>();

            if (Objects.nonNull(chaConfigRichDesc) && Objects.nonNull(chaConfigRichDesc.getChatDetailRichConfig())
                    && Objects.nonNull(chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig())) {
                ChatDefaultRichConfigResp chatDefaultConfig = chaConfigRichDesc.getChatDetailRichConfig()
                        .getChatDefaultConfig();
                if (!CollectionUtils.isEmpty(chatDefaultConfig.getDimensions())) {
                    for (SchemaElement dimensionDesc : chatDefaultConfig.getDimensions()) {
                        DataInfo mainEntityDimension = new DataInfo();
                        mainEntityDimension.setItemId(dimensionDesc.getId().intValue());
                        mainEntityDimension.setName(dimensionDesc.getName());
                        mainEntityDimension.setBizName(dimensionDesc.getBizName());
                        dimensions.add(mainEntityDimension);
                    }
                    entityInfo.setDimensions(dimensions);
                }

                if (!CollectionUtils.isEmpty(chatDefaultConfig.getMetrics())) {
                    for (SchemaElement metricDesc : chatDefaultConfig.getMetrics()) {
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

    public void setMainModel(EntityInfo modelInfo, SemanticParseInfo parseInfo, String entity, User user) {
        if (StringUtils.isEmpty(entity)) {
            return;
        }

        List<String> entities = Collections.singletonList(entity);

        QueryResultWithSchemaResp queryResultWithColumns = getQueryResultWithSchemaResp(modelInfo, parseInfo, entities,
                user);

        if (queryResultWithColumns != null) {
            if (!CollectionUtils.isEmpty(queryResultWithColumns.getResultList())
                    && queryResultWithColumns.getResultList().size() > 0) {
                Map<String, Object> result = queryResultWithColumns.getResultList().get(0);
                for (Map.Entry<String, Object> entry : result.entrySet()) {
                    String entryKey = getEntryKey(entry);
                    if (entry.getValue() == null || entryKey == null) {
                        continue;
                    }
                    modelInfo.getDimensions().stream().filter(i -> entryKey.equals(i.getBizName()))
                            .forEach(i -> i.setValue(entry.getValue().toString()));
                    modelInfo.getMetrics().stream().filter(i -> entryKey.equals(i.getBizName()))
                            .forEach(i -> i.setValue(entry.getValue().toString()));
                }
            }
        }
    }

    public QueryResultWithSchemaResp getQueryResultWithSchemaResp(EntityInfo modelInfo, SemanticParseInfo parseInfo,
            List<String> entities, User user) {
        if (CollectionUtils.isEmpty(entities)) {
            return null;
        }
        ModelSchema modelSchema = schemaService.getModelSchema(parseInfo.getModelId());
        modelInfo.setEntityId(entities.get(0));
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setModel(ModelCluster.build(Sets.newHashSet(parseInfo.getModelId())));
        semanticParseInfo.setQueryType(QueryType.TAG);
        semanticParseInfo.setMetrics(getMetrics(modelInfo));
        semanticParseInfo.setDimensions(getDimensions(modelInfo));
        DateConf dateInfo = new DateConf();
        int unit = 1;
        ChatConfigResp chatConfigInfo =
                configService.fetchConfigByModelId(modelSchema.getModel().getId());
        if (Objects.nonNull(chatConfigInfo) && Objects.nonNull(chatConfigInfo.getChatDetailConfig())
                && Objects.nonNull(chatConfigInfo.getChatDetailConfig().getChatDefaultConfig())) {
            ChatDefaultConfigReq chatDefaultConfig = chatConfigInfo.getChatDetailConfig().getChatDefaultConfig();
            unit = chatDefaultConfig.getUnit();
            String date = LocalDate.now().plusDays(-unit).toString();
            dateInfo.setDateMode(DateMode.BETWEEN);
            dateInfo.setStartDate(date);
            dateInfo.setEndDate(date);
        } else {
            dateInfo.setUnit(unit);
            dateInfo.setDateMode(DateMode.RECENT);
        }
        semanticParseInfo.setDateInfo(dateInfo);

        // add filter
        QueryFilter chatFilter = getQueryFilter(modelInfo, entities);
        Set<QueryFilter> chatFilters = new LinkedHashSet();
        chatFilters.add(chatFilter);
        semanticParseInfo.setDimensionFilters(chatFilters);

        QueryResultWithSchemaResp queryResultWithColumns = null;
        try {
            QueryStructReq queryStructReq = QueryReqBuilder.buildStructReq(semanticParseInfo);
            queryResultWithColumns = semanticInterpreter.queryByStruct(queryStructReq, user);
        } catch (Exception e) {
            log.warn("setMainModel queryByStruct error, e:", e);
        }
        return queryResultWithColumns;
    }

    private QueryFilter getQueryFilter(EntityInfo modelInfo, List<String> entities) {
        QueryFilter chatFilter = new QueryFilter();
        if (entities.size() == 1) {
            chatFilter.setValue(entities.get(0));
            chatFilter.setOperator(FilterOperatorEnum.EQUALS);
        } else {
            chatFilter.setValue(entities);
            chatFilter.setOperator(FilterOperatorEnum.IN);
        }
        chatFilter.setBizName(getEntityPrimaryName(modelInfo));
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

    private String getEntityPrimaryName(EntityInfo modelInfo) {
        return modelInfo.getModelInfo().getPrimaryKey();
    }

}
