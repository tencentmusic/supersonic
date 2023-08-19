package com.tencent.supersonic.chat.parser;


import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.query.dsl.DSLQuery;
import lombok.extern.slf4j.Slf4j;

/**
 * This checker can be used by semantic parsers to check if query intent
 * has already been satisfied by current candidate queries. If so, current
 * parser could be skipped.
 */
@Slf4j
public class SatisfactionChecker {

    private static final double LONG_TEXT_THRESHOLD = 0.8;
    private static final double SHORT_TEXT_THRESHOLD = 0.5;
    private static final int QUERY_TEXT_LENGTH_THRESHOLD = 10;

    // check all the parse info in candidate
    public static boolean check(QueryContext queryContext) {
        for (SemanticQuery query : queryContext.getCandidateQueries()) {
            if (query.getQueryMode().equals(DSLQuery.QUERY_MODE)) {
                continue;
            }
            if (checkThreshold(queryContext.getRequest().getQueryText(), query.getParseInfo())) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkThreshold(String queryText, SemanticParseInfo semanticParseInfo) {
        int queryTextLength = queryText.length();
        double degree = semanticParseInfo.getScore() / queryTextLength;
        if (queryTextLength > QUERY_TEXT_LENGTH_THRESHOLD) {
            if (degree < LONG_TEXT_THRESHOLD) {
                return false;
            }
        } else if (degree < SHORT_TEXT_THRESHOLD) {
            return false;
        }
        log.info("queryMode:{}, degree:{}, parse info:{}",
                semanticParseInfo.getQueryMode(), degree, semanticParseInfo);
        return true;
    }

}
