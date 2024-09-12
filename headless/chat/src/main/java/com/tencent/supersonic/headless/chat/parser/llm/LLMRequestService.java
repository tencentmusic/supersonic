package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import com.tencent.supersonic.headless.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.chat.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_LINKING_VALUE_ENABLE;
import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_STRATEGY_TYPE;

@Slf4j
@Service
public class LLMRequestService {

    @Autowired private ParserConfig parserConfig;

    public boolean isSkip(ChatQueryContext queryCtx) {
        if (!queryCtx.getText2SQLType().enableLLM()) {
            log.info("not enable llm, skip");
            return true;
        }

        if (SatisfactionChecker.isSkip(queryCtx)) {
            log.info("skip {}, queryText:{}", LLMSqlParser.class, queryCtx.getQueryText());
            return true;
        }

        return false;
    }

    public Long getDataSetId(ChatQueryContext queryCtx) {
        DataSetResolver dataSetResolver = ComponentFactory.getModelResolver();
        return dataSetResolver.resolve(queryCtx, queryCtx.getDataSetIds());
    }

    public LLMReq getLlmReq(ChatQueryContext queryCtx, Long dataSetId) {
        List<LLMReq.ElementValue> linkingValues = getValues(queryCtx, dataSetId);
        Map<Long, String> dataSetIdToName = queryCtx.getSemanticSchema().getDataSetIdToName();
        String queryText = queryCtx.getQueryText();

        LLMReq llmReq = new LLMReq();

        llmReq.setQueryText(queryText);
        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setDataSetId(dataSetId);
        llmSchema.setDataSetName(dataSetIdToName.get(dataSetId));
        llmSchema.setMetrics(getMatchedMetrics(queryCtx, dataSetId));
        llmSchema.setDimensions(getMatchedDimensions(queryCtx, dataSetId));
        llmSchema.setPartitionTime(getPartitionTime(queryCtx, dataSetId));
        llmSchema.setPrimaryKey(getPrimaryKey(queryCtx, dataSetId));
        llmSchema.setTerms(getTerms(queryCtx, dataSetId));
        llmReq.setSchema(llmSchema);

        List<LLMReq.ElementValue> linking = new ArrayList<>();
        boolean linkingValueEnabled =
                Boolean.valueOf(parserConfig.getParameterValue(PARSER_LINKING_VALUE_ENABLE));

        if (linkingValueEnabled) {
            linking.addAll(linkingValues);
        }
        llmReq.setLinking(linking);

        llmReq.setCurrentDate(DateUtils.getBeforeDate(0));
        llmReq.setSqlGenType(
                LLMReq.SqlGenType.valueOf(parserConfig.getParameterValue(PARSER_STRATEGY_TYPE)));
        llmReq.setModelConfig(queryCtx.getModelConfig());
        llmReq.setPromptConfig(queryCtx.getPromptConfig());
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

    protected List<LLMReq.Term> getTerms(ChatQueryContext queryCtx, Long dataSetId) {
        List<SchemaElementMatch> matchedElements =
                queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        return matchedElements.stream()
                .filter(
                        schemaElementMatch -> {
                            SchemaElementType elementType =
                                    schemaElementMatch.getElement().getType();
                            return SchemaElementType.TERM.equals(elementType);
                        })
                .map(
                        schemaElementMatch -> {
                            LLMReq.Term term = new LLMReq.Term();
                            term.setName(schemaElementMatch.getElement().getName());
                            term.setDescription(schemaElementMatch.getElement().getDescription());
                            term.setAlias(schemaElementMatch.getElement().getAlias());
                            return term;
                        })
                .collect(Collectors.toList());
    }

    private Map<String, String> getFieldNameToDataFormatTypeMap(SemanticSchema semanticSchema) {
        return semanticSchema.getMetrics().stream()
                .filter(metric -> Objects.nonNull(metric.getDataFormatType()))
                .flatMap(
                        metric -> {
                            Set<Pair<String, String>> fieldFormatPairs = new HashSet<>();
                            String dataFormatType = metric.getDataFormatType();
                            fieldFormatPairs.add(Pair.of(metric.getName(), dataFormatType));
                            List<String> aliasList = metric.getAlias();
                            if (!CollectionUtils.isEmpty(aliasList)) {
                                aliasList.forEach(
                                        alias ->
                                                fieldFormatPairs.add(
                                                        Pair.of(alias, dataFormatType)));
                            }
                            return fieldFormatPairs.stream();
                        })
                .collect(
                        Collectors.toMap(
                                Pair::getLeft,
                                Pair::getRight,
                                (existing, replacement) -> existing));
    }

    public List<LLMReq.ElementValue> getValues(@NotNull ChatQueryContext queryCtx, Long dataSetId) {
        List<SchemaElementMatch> matchedElements =
                queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        Set<LLMReq.ElementValue> valueMatches =
                matchedElements.stream()
                        .filter(elementMatch -> !elementMatch.isInherited())
                        .filter(
                                schemaElementMatch -> {
                                    SchemaElementType type =
                                            schemaElementMatch.getElement().getType();
                                    return SchemaElementType.VALUE.equals(type)
                                            || SchemaElementType.ID.equals(type);
                                })
                        .map(
                                elementMatch -> {
                                    LLMReq.ElementValue elementValue = new LLMReq.ElementValue();
                                    elementValue.setFieldName(elementMatch.getElement().getName());
                                    elementValue.setFieldValue(elementMatch.getWord());
                                    return elementValue;
                                })
                        .collect(Collectors.toSet());
        return new ArrayList<>(valueMatches);
    }

    protected List<SchemaElement> getMatchedMetrics(
            @NotNull ChatQueryContext queryCtx, Long dataSetId) {
        List<SchemaElementMatch> matchedElements =
                queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return Collections.emptyList();
        }
        List<SchemaElement> schemaElements =
                matchedElements.stream()
                        .filter(
                                schemaElementMatch -> {
                                    SchemaElementType elementType =
                                            schemaElementMatch.getElement().getType();
                                    return SchemaElementType.METRIC.equals(elementType);
                                })
                        .map(
                                schemaElementMatch -> {
                                    return schemaElementMatch.getElement();
                                })
                        .collect(Collectors.toList());
        return schemaElements;
    }

    protected List<SchemaElement> getMatchedDimensions(
            @NotNull ChatQueryContext queryCtx, Long dataSetId) {

        List<SchemaElementMatch> matchedElements =
                queryCtx.getMapInfo().getMatchedElements(dataSetId);
        List<SchemaElement> dimensionElements =
                matchedElements.stream()
                        .filter(
                                element ->
                                        SchemaElementType.DIMENSION.equals(
                                                element.getElement().getType()))
                        .map(SchemaElementMatch::getElement)
                        .collect(Collectors.toList());

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
}
