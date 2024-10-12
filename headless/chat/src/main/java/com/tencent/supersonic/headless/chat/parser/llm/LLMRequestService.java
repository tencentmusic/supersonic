package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.chat.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_LINKING_VALUE_ENABLE;
import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_STRATEGY_TYPE;

@Slf4j
@Service
public class LLMRequestService {

    @Autowired
    private ParserConfig parserConfig;

    public boolean isSkip(ChatQueryContext queryCtx) {
        if (!queryCtx.getText2SQLType().enableLLM()) {
            log.info("LLM disabled, skip");
            return true;
        }

        return false;
    }

    public Long getDataSetId(ChatQueryContext queryCtx) {
        DataSetResolver dataSetResolver = ComponentFactory.getModelResolver();
        return dataSetResolver.resolve(queryCtx, queryCtx.getDataSetIds());
    }

    public LLMReq getLlmReq(ChatQueryContext queryCtx, Long dataSetId) {
        Map<Long, String> dataSetIdToName = queryCtx.getSemanticSchema().getDataSetIdToName();
        String queryText = queryCtx.getQueryText();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);
        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmReq.setSchema(llmSchema);
        llmSchema.setDatabaseType(getDatabaseType(queryCtx, dataSetId));
        llmSchema.setDataSetId(dataSetId);
        llmSchema.setDataSetName(dataSetIdToName.get(dataSetId));
        llmSchema.setMetrics(getMappedMetrics(queryCtx, dataSetId));
        llmSchema.setDimensions(getMappedDimensions(queryCtx, dataSetId));
        llmSchema.setPartitionTime(getPartitionTime(queryCtx, dataSetId));
        llmSchema.setPrimaryKey(getPrimaryKey(queryCtx, dataSetId));

        boolean linkingValueEnabled =
                Boolean.valueOf(parserConfig.getParameterValue(PARSER_LINKING_VALUE_ENABLE));
        if (linkingValueEnabled) {
            llmSchema.setValues(getMappedValues(queryCtx, dataSetId));
        }

        llmReq.setCurrentDate(DateUtils.getBeforeDate(0));
        llmReq.setTerms(getMappedTerms(queryCtx, dataSetId));
        llmReq.setSqlGenType(
                LLMReq.SqlGenType.valueOf(parserConfig.getParameterValue(PARSER_STRATEGY_TYPE)));
        llmReq.setChatAppConfig(queryCtx.getChatAppConfig());
        llmReq.setDynamicExemplars(queryCtx.getDynamicExemplars());

        return llmReq;
    }

    public LLMResp runText2SQL(LLMReq llmReq) {
        SqlGenStrategy sqlGenStrategy = SqlGenStrategyFactory.get(llmReq.getSqlGenType());
        String dataSet = llmReq.getSchema().getDataSetName();
        LLMResp result = sqlGenStrategy.generate(llmReq);
        result.setQuery(llmReq.getQueryText());
        result.setDataSet(dataSet);
        return result;
    }

    protected List<LLMReq.Term> getMappedTerms(ChatQueryContext queryCtx, Long dataSetId) {
        List<SchemaElementMatch> matchedElements =
                queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        return matchedElements.stream().filter(schemaElementMatch -> {
            SchemaElementType elementType = schemaElementMatch.getElement().getType();
            return SchemaElementType.TERM.equals(elementType);
        }).map(schemaElementMatch -> {
            LLMReq.Term term = new LLMReq.Term();
            term.setName(schemaElementMatch.getElement().getName());
            term.setDescription(schemaElementMatch.getElement().getDescription());
            term.setAlias(schemaElementMatch.getElement().getAlias());
            return term;
        }).collect(Collectors.toList());
    }

    protected List<LLMReq.ElementValue> getMappedValues(@NotNull ChatQueryContext queryCtx,
            Long dataSetId) {
        List<SchemaElementMatch> matchedElements =
                queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        Set<LLMReq.ElementValue> valueMatches = matchedElements.stream()
                .filter(elementMatch -> !elementMatch.isInherited()).filter(schemaElementMatch -> {
                    SchemaElementType type = schemaElementMatch.getElement().getType();
                    return SchemaElementType.VALUE.equals(type)
                            || SchemaElementType.ID.equals(type);
                }).map(elementMatch -> {
                    LLMReq.ElementValue elementValue = new LLMReq.ElementValue();
                    elementValue.setFieldName(elementMatch.getElement().getName());
                    elementValue.setFieldValue(elementMatch.getWord());
                    return elementValue;
                }).collect(Collectors.toSet());
        return new ArrayList<>(valueMatches);
    }

    protected List<SchemaElement> getMappedMetrics(@NotNull ChatQueryContext queryCtx,
            Long dataSetId) {
        List<SchemaElementMatch> matchedElements =
                queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return Collections.emptyList();
        }
        List<SchemaElement> schemaElements = matchedElements.stream().filter(schemaElementMatch -> {
            SchemaElementType elementType = schemaElementMatch.getElement().getType();
            return SchemaElementType.METRIC.equals(elementType);
        }).map(schemaElementMatch -> {
            return schemaElementMatch.getElement();
        }).collect(Collectors.toList());
        return schemaElements;
    }

    protected List<SchemaElement> getMappedDimensions(@NotNull ChatQueryContext queryCtx,
            Long dataSetId) {

        List<SchemaElementMatch> matchedElements =
                queryCtx.getMapInfo().getMatchedElements(dataSetId);
        List<SchemaElement> dimensionElements = matchedElements.stream().filter(
                element -> SchemaElementType.DIMENSION.equals(element.getElement().getType()))
                .map(SchemaElementMatch::getElement).collect(Collectors.toList());

        return new ArrayList<>(dimensionElements);
    }

    protected SchemaElement getPartitionTime(@NotNull ChatQueryContext queryCtx, Long dataSetId) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        if (semanticSchema == null || semanticSchema.getDataSetSchemaMap() == null) {
            return null;
        }
        Map<Long, DataSetSchema> dataSetSchemaMap = semanticSchema.getDataSetSchemaMap();
        DataSetSchema dataSetSchema = dataSetSchemaMap.get(dataSetId);
        return dataSetSchema.getPartitionDimension();
    }

    protected SchemaElement getPrimaryKey(@NotNull ChatQueryContext queryCtx, Long dataSetId) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        if (semanticSchema == null || semanticSchema.getDataSetSchemaMap() == null) {
            return null;
        }
        Map<Long, DataSetSchema> dataSetSchemaMap = semanticSchema.getDataSetSchemaMap();
        DataSetSchema dataSetSchema = dataSetSchemaMap.get(dataSetId);
        return dataSetSchema.getPrimaryKey();
    }

    protected String getDatabaseType(@NotNull ChatQueryContext queryCtx, Long dataSetId) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        if (semanticSchema == null || semanticSchema.getDataSetSchemaMap() == null) {
            return null;
        }
        Map<Long, DataSetSchema> dataSetSchemaMap = semanticSchema.getDataSetSchemaMap();
        DataSetSchema dataSetSchema = dataSetSchemaMap.get(dataSetId);
        return dataSetSchema.getDatabaseType();
    }
}
