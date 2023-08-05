package com.tencent.supersonic.chat.parser.rule;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricDomainQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricEntityQuery;
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

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;

@Slf4j
public class ContextInheritParser implements SemanticParser {

    private static final Map<SchemaElementType, List<SchemaElementType>> MUTUAL_EXCLUSIVE_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>(METRIC, Arrays.asList(METRIC)),
            new AbstractMap.SimpleEntry<>(DIMENSION, Arrays.asList(DIMENSION, VALUE)),
            new AbstractMap.SimpleEntry<>(VALUE, Arrays.asList(VALUE, DIMENSION)),
            new AbstractMap.SimpleEntry<>(ENTITY, Arrays.asList(ENTITY)),
            new AbstractMap.SimpleEntry<>(DOMAIN, Arrays.asList(DOMAIN)),
            new AbstractMap.SimpleEntry<>(ID, Arrays.asList(ID))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        if (!shouldInherit(queryContext, chatContext)) {
            return;
        }

        Long domainId = chatContext.getParseInfo().getDomainId();
        List<SchemaElementMatch> elementMatches = queryContext.getMapInfo()
                .getMatchedElements(domainId);

        List<SchemaElementMatch> matchesToInherit = new ArrayList<>();
        for (SchemaElementMatch match : chatContext.getParseInfo().getElementMatches()) {
            SchemaElementType matchType = match.getElement().getType();
            // mutual exclusive element types should not be inherited
            RuleSemanticQuery ruleQuery = QueryManager.getRuleQuery(chatContext.getParseInfo().getQueryMode());
            if (!containsTypes(elementMatches, matchType, ruleQuery)) {
                match.setMode(SchemaElementMatch.MatchMode.INHERIT);
                matchesToInherit.add(match);
            }
        }
        elementMatches.addAll(matchesToInherit);

        List<RuleSemanticQuery> queries = RuleSemanticQuery.resolve(elementMatches, queryContext);
        for (RuleSemanticQuery query : queries) {
            query.fillParseInfo(domainId, chatContext);
            queryContext.getCandidateQueries().add(query);
        }
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
        Long contextDomainId = chatContext.getParseInfo().getDomainId();
        if (queryContext.getMapInfo().getMatchedElements(contextDomainId) == null) {
            return false;
        }

        // if candidates have only one MetricDomain mode and context has value filter , count in context
        List<SemanticQuery> candidateQueries = queryContext.getCandidateQueries().stream()
                .filter(semanticQuery -> semanticQuery.getParseInfo().getDomainId().equals(contextDomainId)).collect(
                        Collectors.toList());
        if (candidateQueries.size() == 1 && (candidateQueries.get(0) instanceof MetricDomainQuery)) {
            return true;
        } else {
            return queryContext.getCandidateQueries().size() == 0;
        }
    }

}
