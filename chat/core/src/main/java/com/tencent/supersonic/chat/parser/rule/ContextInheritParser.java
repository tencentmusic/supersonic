package com.tencent.supersonic.chat.parser.rule;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.DIMENSION;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ENTITY;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ID;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.MODEL;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.VALUE;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricEntityQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricModelQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricSemanticQuery;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextInheritParser implements SemanticParser {

    private static final Map<SchemaElementType, List<SchemaElementType>> MUTUAL_EXCLUSIVE_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>(METRIC, Arrays.asList(METRIC)),
            new AbstractMap.SimpleEntry<>(DIMENSION, Arrays.asList(DIMENSION, VALUE)),
            new AbstractMap.SimpleEntry<>(VALUE, Arrays.asList(VALUE, DIMENSION)),
            new AbstractMap.SimpleEntry<>(ENTITY, Arrays.asList(ENTITY)),
            new AbstractMap.SimpleEntry<>(MODEL, Arrays.asList(MODEL)),
            new AbstractMap.SimpleEntry<>(ID, Arrays.asList(ID))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        if (!shouldInherit(queryContext, chatContext)) {
            return;
        }

        Long modelId = chatContext.getParseInfo().getModelId();
        List<SchemaElementMatch> elementMatches = queryContext.getMapInfo()
                .getMatchedElements(modelId);

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

        List<RuleSemanticQuery> queries = RuleSemanticQuery.resolve(elementMatches, queryContext);
        for (RuleSemanticQuery query : queries) {
            query.fillParseInfo(modelId, queryContext, chatContext);
            if (existSameQuery(query.getParseInfo().getModelId(), query.getQueryMode(), queryContext)) {
                continue;
            }
            queryContext.getCandidateQueries().add(query);
        }
    }

    private boolean existSameQuery(Long modelId, String queryMode, QueryContext queryContext) {
        for (SemanticQuery semanticQuery : queryContext.getCandidateQueries()) {
            if (semanticQuery.getQueryMode().equals(queryMode)
                    && semanticQuery.getParseInfo().getModelId().equals(modelId)) {
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
                    && !(ruleQuery instanceof MetricEntityQuery)) {
                return types.contains(type);
            }
            return type.equals(matchType);
        });
    }

    protected boolean shouldInherit(QueryContext queryContext, ChatContext chatContext) {
        Long contextModelId = chatContext.getParseInfo().getModelId();
        // if map info doesn't contain the same Model of the context,
        // no inheritance could be done
        if (queryContext.getMapInfo().getMatchedElements(contextModelId) == null) {
            return false;
        }

        // if candidates only have MetricModel mode, count in context
        List<SemanticQuery> metricModelQueries = queryContext.getCandidateQueries().stream()
                .filter(query -> query instanceof MetricModelQuery).collect(
                        Collectors.toList());
        if (metricModelQueries.size() == queryContext.getCandidateQueries().size()) {
            return true;
        } else {
            return queryContext.getCandidateQueries().size() == 0;
        }
    }

}
