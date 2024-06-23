package com.tencent.supersonic.headless.chat.parser.llm;

import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_LINKING_VALUE_ENABLE;
import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_STRATEGY_TYPE;

import com.tencent.supersonic.common.pojo.enums.DataFormatTypeEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import com.tencent.supersonic.headless.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.chat.utils.ComponentFactory;
import com.tencent.supersonic.headless.chat.utils.S2SqlDateHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class LLMRequestService {

    @Autowired
    private LLMParserConfig llmParserConfig;

    @Autowired
    private ParserConfig parserConfig;

    public boolean isSkip(QueryContext queryCtx) {
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

    public Long getDataSetId(QueryContext queryCtx) {
        DataSetResolver dataSetResolver = ComponentFactory.getModelResolver();
        return dataSetResolver.resolve(queryCtx, queryCtx.getDataSetIds());
    }

    public LLMReq getLlmReq(QueryContext queryCtx, Long dataSetId) {
        LLMRequestService requestService = ContextUtils.getBean(LLMRequestService.class);
        List<LLMReq.ElementValue> linkingValues = requestService.getValues(queryCtx, dataSetId);
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        Map<Long, String> dataSetIdToName = semanticSchema.getDataSetIdToName();
        String queryText = queryCtx.getQueryText();

        LLMReq llmReq = new LLMReq();

        llmReq.setQueryText(queryText);
        LLMReq.FilterCondition filterCondition = new LLMReq.FilterCondition();
        llmReq.setFilterCondition(filterCondition);

        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setDataSetId(dataSetId);
        llmSchema.setDataSetName(dataSetIdToName.get(dataSetId));
        llmSchema.setDomainName(dataSetIdToName.get(dataSetId));

        List<String> fieldNameList = getFieldNameList(queryCtx, dataSetId, llmParserConfig);
        fieldNameList.add(TimeDimensionEnum.DAY.getChName());
        llmSchema.setFieldNameList(fieldNameList);

        llmSchema.setMetrics(getMatchedMetrics(queryCtx, dataSetId));
        llmSchema.setDimensions(getMatchedDimensions(queryCtx, dataSetId));
        llmSchema.setTerms(getTerms(queryCtx, dataSetId));
        llmReq.setSchema(llmSchema);

        String priorExts = getPriorExts(queryCtx, fieldNameList);
        llmReq.setPriorExts(priorExts);

        List<LLMReq.ElementValue> linking = new ArrayList<>();
        boolean linkingValueEnabled = Boolean.valueOf(parserConfig.getParameterValue(PARSER_LINKING_VALUE_ENABLE));

        if (linkingValueEnabled) {
            linking.addAll(linkingValues);
        }
        llmReq.setLinking(linking);

        String currentDate = S2SqlDateHelper.getReferenceDate(queryCtx, dataSetId);
        if (StringUtils.isEmpty(currentDate)) {
            currentDate = DateUtils.getBeforeDate(0);
        }
        llmReq.setCurrentDate(currentDate);
        llmReq.setSqlGenType(LLMReq.SqlGenType.valueOf(parserConfig.getParameterValue(PARSER_STRATEGY_TYPE)));
        llmReq.setLlmConfig(queryCtx.getLlmConfig());

        return llmReq;
    }

    public LLMResp runText2SQL(LLMReq llmReq) {
        return ComponentFactory.getLLMProxy().text2sql(llmReq);
    }

    protected List<String> getFieldNameList(QueryContext queryCtx, Long dataSetId,
            LLMParserConfig llmParserConfig) {

        Set<String> results = getTopNFieldNames(queryCtx, dataSetId, llmParserConfig);

        Set<String> fieldNameList = getMatchedFieldNames(queryCtx, dataSetId);

        results.addAll(fieldNameList);
        return new ArrayList<>(results);
    }

    protected List<LLMReq.Term> getTerms(QueryContext queryCtx, Long dataSetId) {
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        return matchedElements.stream()
                .filter(schemaElementMatch -> {
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

    private String getPriorExts(QueryContext queryContext, List<String> fieldNameList) {
        StringBuilder extraInfoSb = new StringBuilder();
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        Map<String, String> fieldNameToDataFormatType = semanticSchema.getMetrics()
                .stream().filter(metricSchemaResp -> Objects.nonNull(metricSchemaResp.getDataFormatType()))
                .flatMap(metricSchemaResp -> {
                    Set<Pair<String, String>> result = new HashSet<>();
                    String dataFormatType = metricSchemaResp.getDataFormatType();
                    result.add(Pair.of(metricSchemaResp.getName(), dataFormatType));
                    List<String> aliasList = metricSchemaResp.getAlias();
                    if (!CollectionUtils.isEmpty(aliasList)) {
                        for (String alias : aliasList) {
                            result.add(Pair.of(alias, dataFormatType));
                        }
                    }
                    return result.stream();
                }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        for (String fieldName : fieldNameList) {
            String dataFormatType = fieldNameToDataFormatType.get(fieldName);
            if (DataFormatTypeEnum.DECIMAL.getName().equalsIgnoreCase(dataFormatType)
                    || DataFormatTypeEnum.PERCENT.getName().equalsIgnoreCase(dataFormatType)) {
                String format = String.format("%s的计量单位是%s", fieldName, "小数; ");
                extraInfoSb.append(format);
            }
        }
        return extraInfoSb.toString();
    }

    public List<LLMReq.ElementValue> getValues(QueryContext queryCtx, Long dataSetId) {
        Map<Long, String> itemIdToName = getItemIdToName(queryCtx, dataSetId);
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        Set<LLMReq.ElementValue> valueMatches = matchedElements
                .stream()
                .filter(elementMatch -> !elementMatch.isInherited())
                .filter(schemaElementMatch -> {
                    SchemaElementType type = schemaElementMatch.getElement().getType();
                    return SchemaElementType.VALUE.equals(type) || SchemaElementType.ID.equals(type);
                })
                .map(elementMatch -> {
                    LLMReq.ElementValue elementValue = new LLMReq.ElementValue();
                    elementValue.setFieldName(itemIdToName.get(elementMatch.getElement().getId()));
                    elementValue.setFieldValue(elementMatch.getWord());
                    return elementValue;
                }).collect(Collectors.toSet());
        return new ArrayList<>(valueMatches);
    }

    protected Map<Long, String> getItemIdToName(QueryContext queryCtx, Long dataSetId) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        List<SchemaElement> elements = semanticSchema.getDimensions(dataSetId);
        return elements.stream()
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));
    }

    private Set<String> getTopNFieldNames(QueryContext queryCtx, Long dataSetId, LLMParserConfig llmParserConfig) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        Set<String> results = new HashSet<>();
        Set<String> dimensions = semanticSchema.getDimensions(dataSetId).stream()
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(llmParserConfig.getDimensionTopN())
                .map(entry -> entry.getName())
                .collect(Collectors.toSet());
        results.addAll(dimensions);
        Set<String> metrics = semanticSchema.getMetrics(dataSetId).stream()
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(llmParserConfig.getMetricTopN())
                .map(entry -> entry.getName())
                .collect(Collectors.toSet());
        results.addAll(metrics);
        return results;
    }

    protected List<SchemaElement> getMatchedMetrics(QueryContext queryCtx, Long dataSetId) {
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return Collections.emptyList();
        }
        List<SchemaElement> schemaElements = matchedElements.stream()
                .filter(schemaElementMatch -> {
                    SchemaElementType elementType = schemaElementMatch.getElement().getType();
                    return SchemaElementType.METRIC.equals(elementType);
                })
                .map(schemaElementMatch -> {
                    return schemaElementMatch.getElement();
                })
                .collect(Collectors.toList());
        return schemaElements;
    }

    protected List<SchemaElement> getMatchedDimensions(QueryContext queryCtx, Long dataSetId) {
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return Collections.emptyList();
        }
        List<SchemaElement> schemaElements = matchedElements.stream()
                .filter(schemaElementMatch -> {
                    SchemaElementType elementType = schemaElementMatch.getElement().getType();
                    return SchemaElementType.DIMENSION.equals(elementType);
                })
                .map(schemaElementMatch -> {
                    return schemaElementMatch.getElement();
                })
                .collect(Collectors.toList());
        return schemaElements;
    }

    protected Set<String> getMatchedFieldNames(QueryContext queryCtx, Long dataSetId) {
        Map<Long, String> itemIdToName = getItemIdToName(queryCtx, dataSetId);
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new HashSet<>();
        }
        Set<String> fieldNameList = matchedElements.stream()
                .filter(schemaElementMatch -> {
                    SchemaElementType elementType = schemaElementMatch.getElement().getType();
                    return SchemaElementType.METRIC.equals(elementType)
                            || SchemaElementType.DIMENSION.equals(elementType)
                            || SchemaElementType.VALUE.equals(elementType);
                })
                .map(schemaElementMatch -> {
                    SchemaElement element = schemaElementMatch.getElement();
                    SchemaElementType elementType = element.getType();
                    if (SchemaElementType.VALUE.equals(elementType)) {
                        return itemIdToName.get(element.getId());
                    }
                    return schemaElementMatch.getWord();
                })
                .collect(Collectors.toSet());
        return fieldNameList;
    }
}
