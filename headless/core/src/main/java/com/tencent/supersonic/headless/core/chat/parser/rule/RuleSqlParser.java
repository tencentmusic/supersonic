package com.tencent.supersonic.headless.core.chat.parser.rule;

import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.core.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.core.chat.query.QueryManager;
import com.tencent.supersonic.headless.core.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * RuleSqlParser resolves a specific SemanticQuery according to co-appearance
 * of certain schema element types.
 */
@Slf4j
public class RuleSqlParser implements SemanticParser {

    private static List<SemanticParser> auxiliaryParsers = Arrays.asList(
            new ContextInheritParser(),
            new TimeRangeParser(),
            new AggregateTypeParser()
    );

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        // iterate all schemaElementMatches to resolve query mode
        for (Long dataSetId : mapInfo.getMatchedDataSetInfos()) {
            List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(dataSetId);
            List<RuleSemanticQuery> queries = RuleSemanticQuery.resolve(dataSetId, elementMatches, queryContext);
            for (RuleSemanticQuery query : queries) {
                query.fillParseInfo(queryContext, chatContext);
                SemanticParseInfo parseInfo = query.getParseInfo();
                QueryType queryType = queryContext.getQueryType(parseInfo.getDataSetId());
                if (isRightQuery(parseInfo, queryType)) {
                    queryContext.getCandidateQueries().add(query);
                }
            }
        }

        auxiliaryParsers.stream().forEach(p -> p.parse(queryContext, chatContext));
    }

    private boolean isRightQuery(SemanticParseInfo parseInfo, QueryType queryType) {
        if (QueryType.TAG.equals(queryType) && QueryManager.isTagQuery(parseInfo.getQueryMode())) {
            return true;
        }
        if (QueryType.METRIC.equals(queryType) && QueryManager.isMetricQuery(parseInfo.getQueryMode())) {
            return true;
        }
        return false;
    }
}
