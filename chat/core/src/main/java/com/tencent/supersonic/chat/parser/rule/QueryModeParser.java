package com.tencent.supersonic.chat.parser.rule;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class QueryModeParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        // iterate all schemaElementMatches to resolve semantic query
        for (Long modelId : mapInfo.getMatchedModels()) {
            List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(modelId);
            List<RuleSemanticQuery> queries = RuleSemanticQuery.resolve(elementMatches, queryContext);
            for (RuleSemanticQuery query : queries) {
                query.fillParseInfo(modelId, queryContext, chatContext);
                queryContext.getCandidateQueries().add(query);
            }
        }
        // if modelElementMatches id empty,so remove it.
        Map<Long, List<SchemaElementMatch>> filterModelElementMatches = new HashMap<>();
        for (Long modelId : queryContext.getMapInfo().getModelElementMatches().keySet()) {
            if (!CollectionUtils.isEmpty(queryContext.getMapInfo().getModelElementMatches().get(modelId))) {
                filterModelElementMatches.put(modelId, queryContext.getMapInfo().getModelElementMatches().get(modelId));
            }
        }
        queryContext.getMapInfo().setModelElementMatches(filterModelElementMatches);
    }

}
