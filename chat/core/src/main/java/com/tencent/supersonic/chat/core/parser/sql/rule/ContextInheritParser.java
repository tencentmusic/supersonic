package com.tencent.supersonic.chat.core.parser.sql.rule;

import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.core.parser.SemanticParser;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.QueryManager;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.chat.core.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.core.query.rule.metric.MetricModelQuery;
import com.tencent.supersonic.chat.core.query.rule.metric.MetricSemanticQuery;
import com.tencent.supersonic.chat.core.query.rule.metric.MetricTagQuery;
import com.tencent.supersonic.chat.core.utils.ModelClusterBuilder;
import com.tencent.supersonic.common.pojo.ModelCluster;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
            new AbstractMap.SimpleEntry<>(SchemaElementType.TAG, Arrays.asList(SchemaElementType.TAG)),
            new AbstractMap.SimpleEntry<>(SchemaElementType.MODEL, Arrays.asList(SchemaElementType.MODEL)),
            new AbstractMap.SimpleEntry<>(SchemaElementType.ID, Arrays.asList(SchemaElementType.ID))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        if (!shouldInherit(queryContext)) {
            return;
        }
        ModelCluster modelCluster = getMatchedModelCluster(queryContext, chatContext);
        if (modelCluster == null) {
            return;
        }
        List<SchemaElementMatch> elementMatches = queryContext.getModelClusterMapInfo()
                .getMatchedElements(modelCluster.getKey());
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
            query.fillParseInfo(queryContext, chatContext);
            if (existSameQuery(query.getParseInfo().getModelClusterKey(), query.getQueryMode(), queryContext)) {
                continue;
            }
            queryContext.getCandidateQueries().add(query);
        }
    }

    private boolean existSameQuery(String modelClusterKey, String queryMode, QueryContext queryContext) {
        for (SemanticQuery semanticQuery : queryContext.getCandidateQueries()) {
            if (semanticQuery.getQueryMode().equals(queryMode)
                    && semanticQuery.getParseInfo().getModelClusterKey().equals(modelClusterKey)) {
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
                    && !(ruleQuery instanceof MetricTagQuery)) {
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

    protected ModelCluster getMatchedModelCluster(QueryContext queryContext, ChatContext chatContext) {
        String contextModelClusterKey = chatContext.getParseInfo().getModelClusterKey();
        if (StringUtils.isBlank(contextModelClusterKey)) {
            return null;
        }
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        List<ModelCluster> allModelClusters = ModelClusterBuilder.buildModelClusters(semanticSchema);
        Set<String> queryModelClusters = queryContext.getModelClusterMapInfo().getMatchedModelClusters();
        ModelCluster contextModelCluster = ModelCluster.build(contextModelClusterKey);
        for (String cluster : queryModelClusters) {
            ModelCluster queryModelCluster = ModelCluster.build(cluster);
            for (ModelCluster modelCluster : allModelClusters) {
                if (modelCluster.getModelIds().containsAll(contextModelCluster.getModelIds())
                        && modelCluster.getModelIds().containsAll(queryModelCluster.getModelIds())) {
                    return queryModelCluster;
                }
            }
        }
        return null;
    }

}
