package com.tencent.supersonic.chat.parser.sql.rule;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaModelClusterMapInfo;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * RuleSqlParser resolves a specific SemanticQuery according to co-appearance
 * of certain schema element types.
 */
@Slf4j
public class RuleSqlParser implements SemanticParser {

    private static List<SemanticParser> auxiliaryParsers = Arrays.asList(
            new ContextInheritParser(),
            new AgentCheckParser(),
            new TimeRangeParser(),
            new AggregateTypeParser()
    );

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        SchemaModelClusterMapInfo modelClusterMapInfo = queryContext.getModelClusterMapInfo();
        // iterate all schemaElementMatches to resolve query mode
        for (String modelClusterKey : modelClusterMapInfo.getMatchedModelClusters()) {
            List<SchemaElementMatch> elementMatches = modelClusterMapInfo.getMatchedElements(modelClusterKey);
            List<RuleSemanticQuery> queries = RuleSemanticQuery.resolve(elementMatches, queryContext);
            for (RuleSemanticQuery query : queries) {
                query.fillParseInfo(chatContext);
                queryContext.getCandidateQueries().add(query);
            }
        }

        auxiliaryParsers.stream().forEach(p -> p.parse(queryContext, chatContext));
    }
}
