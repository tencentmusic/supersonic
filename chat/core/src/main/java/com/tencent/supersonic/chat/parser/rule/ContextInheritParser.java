package com.tencent.supersonic.chat.parser.rule;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.query.rule.metric.MetricDomainQuery;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.common.util.JsonUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;

@Slf4j
public class ContextInheritParser implements SemanticParser {

    private static final Map<SchemaElementType, List<SchemaElementType>> MUTUAL_EXCLUSIVE_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>(METRIC, Arrays.asList(METRIC)),
            new AbstractMap.SimpleEntry<>(DIMENSION, Arrays.asList(DIMENSION, VALUE)),
            new AbstractMap.SimpleEntry<>(VALUE, Arrays.asList(VALUE, DIMENSION)),
            new AbstractMap.SimpleEntry<>(ENTITY, Arrays.asList(ENTITY)),
            new AbstractMap.SimpleEntry<>(DOMAIN, Arrays.asList(DOMAIN))
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
            if (!containsTypes(elementMatches, MUTUAL_EXCLUSIVE_MAP.get(matchType))) {
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

    private boolean containsTypes(List<SchemaElementMatch> matches, List<SchemaElementType> types) {
        return matches.stream().anyMatch(m -> types.contains(m.getElement().getType()));
    }

    protected boolean shouldInherit(QueryContext queryContext, ChatContext chatContext) {
        if (queryContext.getMapInfo().getMatchedElements(
                chatContext.getParseInfo().getDomainId()) == null) {
            return false;
        }

        // if candidates have only one MetricDomain mode and context has value filter , count in context
        if (queryContext.getCandidateQueries().size() == 1 && (queryContext.getCandidateQueries()
                .get(0) instanceof MetricDomainQuery)
                && queryContext.getCandidateQueries().get(0).getParseInfo().getDomainId()
                .equals(chatContext.getParseInfo().getDomainId())
                && !CollectionUtils.isEmpty(chatContext.getParseInfo().getDimensionFilters())) {
            return true;
        } else {
            return queryContext.getCandidateQueries().size() == 0;
        }
    }

}
