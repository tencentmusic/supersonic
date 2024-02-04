package com.tencent.supersonic.chat.core.parser.sql.rule;

import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.core.parser.SemanticParser;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.core.query.rule.RuleSemanticQuery;
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
        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        // iterate all schemaElementMatches to resolve query mode
        for (Long viewId : mapInfo.getMatchedViewInfos()) {
            List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(viewId);
            List<RuleSemanticQuery> queries = RuleSemanticQuery.resolve(elementMatches, queryContext);
            for (RuleSemanticQuery query : queries) {
                query.fillParseInfo(queryContext, chatContext);
                queryContext.getCandidateQueries().add(query);
            }
        }

        auxiliaryParsers.stream().forEach(p -> p.parse(queryContext, chatContext));
    }
}
