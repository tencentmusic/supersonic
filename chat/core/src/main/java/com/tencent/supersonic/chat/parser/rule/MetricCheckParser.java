package com.tencent.supersonic.chat.parser.rule;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.RelateSchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.query.rule.metric.MetricSemanticQuery;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import org.apache.commons.collections.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MetricCheckParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        List<SemanticQuery> semanticQueries = queryContext.getCandidateQueries();
        if (CollectionUtils.isEmpty(semanticQueries)) {
            return;
        }
        semanticQueries.removeIf(this::removeQuery);
    }

    private boolean removeQuery(SemanticQuery semanticQuery) {
        if (semanticQuery instanceof MetricSemanticQuery) {
            SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
            List<SchemaElementMatch> schemaElementMatches = parseInfo.getElementMatches();
            List<SchemaElementMatch> elementMatchFiltered =
                    filterMetricElement(schemaElementMatches, parseInfo.getModelId());
            return 0 >= getMetricElementMatchCount(elementMatchFiltered);
        }
        return false;
    }

    private List<SchemaElementMatch> filterMetricElement(List<SchemaElementMatch> elementMatches, Long modelId) {
        List<SchemaElementMatch> filterSchemaElementMatch = Lists.newArrayList();
        SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();
        ModelSchema modelSchema = semanticInterpreter.getModelSchema(modelId, true);
        Set<SchemaElement> metricElements = modelSchema.getMetrics();
        Map<Long, SchemaElementMatch> valueElementMatchMap = getValueElementMap(elementMatches);
        Map<Long, SchemaElement> metricMap = metricElements.stream()
                .collect(Collectors.toMap(SchemaElement::getId, e -> e, (e1, e2) -> e2));
        for (SchemaElementMatch schemaElementMatch : elementMatches) {
            if (!SchemaElementType.METRIC.equals(schemaElementMatch.getElement().getType())) {
                filterSchemaElementMatch.add(schemaElementMatch);
                continue;
            }
            SchemaElement metric = metricMap.get(schemaElementMatch.getElement().getId());
            List<Long> necessaryDimensionIds = getNecessaryDimensionIds(metric);
            boolean flag = true;
            for (Long necessaryDimensionId : necessaryDimensionIds) {
                if (!valueElementMatchMap.containsKey(necessaryDimensionId)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                filterSchemaElementMatch.add(schemaElementMatch);
            }
        }
        return filterSchemaElementMatch;
    }

    private Map<Long, SchemaElementMatch> getValueElementMap(List<SchemaElementMatch> elementMatches) {
        return elementMatches.stream()
                .filter(elementMatch ->
                        SchemaElementType.VALUE.equals(elementMatch.getElement().getType()))
                .collect(Collectors.toMap(elementMatch -> elementMatch.getElement().getId(), e -> e, (e1, e2) -> e1));
    }

    private long getMetricElementMatchCount(List<SchemaElementMatch> elementMatches) {
        return elementMatches.stream().filter(elementMatch ->
                SchemaElementType.METRIC.equals(elementMatch.getElement().getType()))
                .count();
    }

    private List<Long> getNecessaryDimensionIds(SchemaElement metric) {
        if (metric == null) {
            return Lists.newArrayList();
        }
        List<RelateSchemaElement> relateSchemaElements = metric.getRelateSchemaElements();
        if (CollectionUtils.isEmpty(relateSchemaElements)) {
            return Lists.newArrayList();
        }
        return relateSchemaElements.stream()
                .filter(RelateSchemaElement::isNecessary).map(RelateSchemaElement::getDimensionId)
                .collect(Collectors.toList());
    }

}