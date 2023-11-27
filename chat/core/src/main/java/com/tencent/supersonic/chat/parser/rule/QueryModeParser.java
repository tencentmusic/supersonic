package com.tencent.supersonic.chat.parser.rule;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaModelClusterMapInfo;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * QueryModeParser resolves a specific query mode according to co-appearance
 * of certain schema element types.
 */
@Slf4j
public class QueryModeParser implements SemanticParser {

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
    }

}
