package com.tencent.supersonic.headless.chat.parser.rule;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.rule.RuleSemanticQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * RuleSqlParser resolves a specific SemanticQuery according to co-appearance of certain schema
 * element types.
 */
@Slf4j
public class RuleSqlParser implements SemanticParser {

    private static final List<SemanticParser> auxiliaryParsers = Arrays
            .asList(new ContextInheritParser(), new TimeRangeParser(), new AggregateTypeParser());

    @Override
    public void parse(ChatQueryContext chatQueryContext) {
        if (!chatQueryContext.getRequest().getText2SQLType().enableRule()
                || !chatQueryContext.getCandidateQueries().isEmpty()) {
            return;
        }
        SchemaMapInfo mapInfo = chatQueryContext.getMapInfo();
        List<SemanticQuery> candidateQueries = Lists.newArrayList();
        // iterate all schemaElementMatches to resolve query mode
        for (Long dataSetId : mapInfo.getMatchedDataSetInfos()) {
            List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(dataSetId);
            List<RuleSemanticQuery> queries =
                    RuleSemanticQuery.resolve(dataSetId, elementMatches, chatQueryContext);
            for (RuleSemanticQuery query : queries) {
                query.fillParseInfo(chatQueryContext);
                chatQueryContext.getCandidateQueries().add(query);
            }
            candidateQueries.addAll(chatQueryContext.getCandidateQueries());
            chatQueryContext.getCandidateQueries().clear();
        }
        chatQueryContext.setCandidateQueries(candidateQueries);

        auxiliaryParsers.forEach(p -> p.parse(chatQueryContext));
    }
}
