package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import lombok.extern.slf4j.Slf4j;

import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_TEXT_LENGTH_THRESHOLD;
import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_TEXT_LENGTH_THRESHOLD_LONG;
import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_TEXT_LENGTH_THRESHOLD_SHORT;


/**
 * This checker can be used by semantic parsers to check if query intent
 * has already been satisfied by current candidate queries. If so, current
 * parser could be skipped.
 */
@Slf4j
public class SatisfactionChecker {

    // check all the parse info in candidate
    public static boolean isSkip(QueryContext queryContext) {
        for (SemanticQuery query : queryContext.getCandidateQueries()) {
            if (query.getQueryMode().equals(LLMSqlQuery.QUERY_MODE)) {
                continue;
            }
            if (checkThreshold(queryContext.getQueryText(), query.getParseInfo())) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkThreshold(String queryText, SemanticParseInfo semanticParseInfo) {
        int queryTextLength = queryText.replaceAll(" ", "").length();
        double degree = semanticParseInfo.getScore() / queryTextLength;
        ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
        int textLengthThreshold =
                Integer.valueOf(parserConfig.getParameterValue(PARSER_TEXT_LENGTH_THRESHOLD));
        double longTextLengthThreshold =
                Double.valueOf(parserConfig.getParameterValue(PARSER_TEXT_LENGTH_THRESHOLD_LONG));
        double shortTextLengthThreshold =
                Double.valueOf(parserConfig.getParameterValue(PARSER_TEXT_LENGTH_THRESHOLD_SHORT));

        if (queryTextLength > textLengthThreshold) {
            if (degree < longTextLengthThreshold) {
                return false;
            }
        } else if (degree < shortTextLengthThreshold) {
            return false;
        }
        log.info("queryMode:{}, degree:{}, parse info:{}",
                semanticParseInfo.getQueryMode(), degree, semanticParseInfo);
        return true;
    }

}
