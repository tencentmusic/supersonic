package com.tencent.supersonic.chat.domain.utils;


import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * utils to check current parse info is enough to query result
 */
@Slf4j
public class SemanticSatisfactionChecker {

    private static final double THRESHOLD = 0.8;

    // check all the parse info in candidate
    public static boolean check(QueryContextReq queryCtx) {
        for (SemanticQuery query : queryCtx.getCandidateQueries()) {
            SemanticParseInfo semanticParseInfo = query.getParseInfo();
            Long domainId = semanticParseInfo.getDomainId();
            List<SchemaElementMatch> schemaElementMatches = queryCtx.getMapInfo()
                    .getMatchedElements(domainId.intValue());
            if (check(queryCtx.getQueryText(), semanticParseInfo, schemaElementMatches)) {
                return true;
            }
        }
        return false;
    }

    //check single parse info
    private static boolean check(String text, SemanticParseInfo semanticParseInfo,
            List<SchemaElementMatch> schemaElementMatches) {
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            return false;
        }
        List<String> detectWords = Lists.newArrayList();
        Map<Integer, String> detectWordMap = schemaElementMatches.stream()
                .collect(Collectors.toMap(SchemaElementMatch::getElementID, SchemaElementMatch::getDetectWord,
                        (id1, id2) -> id1));
        // get detect word in text by element id in semantic layer
        Long domainId = semanticParseInfo.getDomainId();
        if (domainId != null && domainId > 0) {
            for (SchemaElementMatch schemaElementMatch : schemaElementMatches) {
                if (SchemaElementType.DOMAIN.equals(schemaElementMatch.getElementType())) {
                    detectWords.add(schemaElementMatch.getDetectWord());
                }
            }
        }

        for (Filter filter : semanticParseInfo.getDimensionFilters()) {
            detectWords.add(
                    detectWordMap.getOrDefault(Optional.ofNullable(filter.getElementID()).orElse(0L).intValue(), ""));
        }
        for (SchemaItem schemaItem : semanticParseInfo.getMetrics()) {
            detectWords.add(
                    detectWordMap.getOrDefault(Optional.ofNullable(schemaItem.getId()).orElse(0L).intValue(), ""));
            // only first metric
            break;
        }
        for (SchemaItem schemaItem : semanticParseInfo.getDimensions()) {
            detectWords.add(
                    detectWordMap.getOrDefault(Optional.ofNullable(schemaItem.getId()).orElse(0L).intValue(), ""));
            // only first dimension
            break;
        }
        //compare the length between detect words and query text
        String detectWordsDistinct = StringUtils.join(new HashSet<>(detectWords), "");
        int detectWordsLength = detectWordsDistinct.length();
        int queryTextLength = text.length();
        double degree = detectWordsLength * 1.0 / queryTextLength;
        if (degree > THRESHOLD) {
            log.info("queryMode:{} has satisfied semantic check, degree:{}, detectWords:{}, parse info:{}",
                    semanticParseInfo.getQueryMode(), degree, detectWordsDistinct, semanticParseInfo);
            return true;
        }
        return false;
    }

}
