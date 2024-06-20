package com.tencent.supersonic.headless.chat.parser.rule;

import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.headless.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.query.rule.metric.MetricModelQuery;
import com.tencent.supersonic.headless.chat.query.rule.metric.MetricSemanticQuery;
import com.tencent.supersonic.headless.chat.query.rule.metric.MetricIdQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ContextInheritParser tries to inherit certain schema elements from context
 * so that in multi-turn conversations users don't need to mention some keyword
 * repeatedly.
 */
@Slf4j
public class ContextInheritParser implements SemanticParser {

    private static final Map<SchemaElementType, List<SchemaElementType>> MUTUAL_EXCLUSIVE_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>(SchemaElementType.METRIC, Arrays.asList(SchemaElementType.METRIC)),
            new AbstractMap.SimpleEntry<>(
                    SchemaElementType.DIMENSION, Arrays.asList(SchemaElementType.DIMENSION, SchemaElementType.VALUE)),
            new AbstractMap.SimpleEntry<>(
                    SchemaElementType.VALUE, Arrays.asList(SchemaElementType.VALUE, SchemaElementType.DIMENSION)),
            new AbstractMap.SimpleEntry<>(SchemaElementType.ENTITY, Arrays.asList(SchemaElementType.ENTITY)),
            new AbstractMap.SimpleEntry<>(SchemaElementType.DATASET, Arrays.asList(SchemaElementType.DATASET)),
            new AbstractMap.SimpleEntry<>(SchemaElementType.ID, Arrays.asList(SchemaElementType.ID))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        if (!shouldInherit(queryContext)) {
            return;
        }
        Long dataSetId = getMatchedDataSet(queryContext, chatContext);
        if (dataSetId == null) {
            return;
        }

        List<SchemaElementMatch> elementMatches = queryContext.getMapInfo().getMatchedElements(dataSetId);

        List<SchemaElementMatch> matchesToInherit = new ArrayList<>();
        for (SchemaElementMatch match : chatContext.getParseInfo().getElementMatches()) {
            SchemaElementType matchType = match.getElement().getType();
            // mutual exclusive element types should not be inherited
            RuleSemanticQuery ruleQuery = QueryManager.getRuleQuery(chatContext.getParseInfo().getQueryMode());
            if (!containsTypes(elementMatches, matchType, ruleQuery)) {
                match.setInherited(true);
                matchesToInherit.add(match);
            }
        }
        elementMatches.addAll(matchesToInherit);

        List<RuleSemanticQuery> queries = RuleSemanticQuery.resolve(dataSetId, elementMatches, queryContext);
        for (RuleSemanticQuery query : queries) {
            query.fillParseInfo(queryContext, chatContext);
            if (existSameQuery(query.getParseInfo().getDataSetId(), query.getQueryMode(), queryContext)) {
                continue;
            }
            queryContext.getCandidateQueries().add(query);
        }
    }

    private boolean existSameQuery(Long dataSetId, String queryMode, QueryContext queryContext) {
        for (SemanticQuery semanticQuery : queryContext.getCandidateQueries()) {
            if (semanticQuery.getQueryMode().equals(queryMode)
                    && semanticQuery.getParseInfo().getDataSetId().equals(dataSetId)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTypes(List<SchemaElementMatch> matches, SchemaElementType matchType,
            RuleSemanticQuery ruleQuery) {
        List<SchemaElementType> types = MUTUAL_EXCLUSIVE_MAP.get(matchType);

        return matches.stream().anyMatch(m -> {
            SchemaElementType type = m.getElement().getType();
            if (Objects.nonNull(ruleQuery) && ruleQuery instanceof MetricSemanticQuery
                    && !(ruleQuery instanceof MetricIdQuery)) {
                return types.contains(type);
            }
            return type.equals(matchType);
        });
    }

    protected boolean shouldInherit(QueryContext queryContext) {
        // if candidates only have MetricModel mode, count in context
        List<SemanticQuery> metricModelQueries = queryContext.getCandidateQueries().stream()
                .filter(query -> query instanceof MetricModelQuery).collect(
                        Collectors.toList());
        return metricModelQueries.size() == queryContext.getCandidateQueries().size();
    }

    protected Long getMatchedDataSet(QueryContext queryContext, ChatContext chatContext) {
        Long dataSetId = chatContext.getParseInfo().getDataSetId();
        if (dataSetId == null) {
            return null;
        }
        Set<Long> queryDataSets = queryContext.getMapInfo().getMatchedDataSetInfos();
        if (queryDataSets.contains(dataSetId)) {
            return dataSetId;
        }
        return dataSetId;
    }

}
