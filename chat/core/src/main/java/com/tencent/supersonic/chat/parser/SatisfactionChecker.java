package com.tencent.supersonic.chat.parser;


import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.JsonUtil;
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
 * This checker can be used by semantic parsers to check if query intent
 * has already been satisfied by current candidate queries. If so, current
 * parser could be skipped.
 */
@Slf4j
public class SatisfactionChecker {

    private static final double LONG_TEXT_THRESHOLD = 0.8;
    private static final double SHORT_TEXT_THRESHOLD = 0.5;
    private static final int QUERY_TEXT_LENGTH_THRESHOLD = 10;
    public static final double EMBEDDING_THRESHOLD = 0.2;

    // check all the parse info in candidate
    public static boolean check(QueryContext queryCtx) {
        for (SemanticQuery query : queryCtx.getCandidateQueries()) {
            if (query instanceof RuleSemanticQuery) {
                if (checkRuleThreshHold(queryCtx.getRequest().getQueryText(), query.getParseInfo())) {
                    return true;
                }
            } else if (query instanceof PluginSemanticQuery) {
                if (checkEmbeddingThreshold(query.getParseInfo())) {
                    log.info("query mode :{} satisfy check", query.getQueryMode());
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkEmbeddingThreshold(SemanticParseInfo semanticParseInfo) {
        Object object = semanticParseInfo.getProperties().get(Constants.CONTEXT);
        PluginParseResult pluginParseResult = JsonUtil.toObject(JsonUtil.toString(object), PluginParseResult.class);
        return EMBEDDING_THRESHOLD > pluginParseResult.getDistance();
    }

    //check single parse info
    private static boolean checkRuleThreshHold(String text, SemanticParseInfo semanticParseInfo) {
        List<SchemaElementMatch> schemaElementMatches = semanticParseInfo.getElementMatches();
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            return false;
        }
        List<String> detectWords = Lists.newArrayList();
        Map<Long, String> detectWordMap = schemaElementMatches.stream()
                .collect(Collectors.toMap(m -> m.getElement().getId(), SchemaElementMatch::getDetectWord,
                        (id1, id2) -> id1));
        // get detect word in text by element id in semantic layer
        Long domainId = semanticParseInfo.getDomainId();
        if (domainId != null && domainId > 0) {
            for (SchemaElementMatch schemaElementMatch : schemaElementMatches) {
                if (SchemaElementType.DOMAIN.equals(schemaElementMatch.getElement().getType())) {
                    detectWords.add(schemaElementMatch.getDetectWord());
                }
            }
        }
        for (SchemaElementMatch schemaElementMatch : schemaElementMatches) {
            if (SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())) {
                detectWords.add(schemaElementMatch.getDetectWord());
            }
        }
        for (SchemaElementMatch schemaElementMatch : schemaElementMatches) {
            if (SchemaElementType.ID.equals(schemaElementMatch.getElement().getType())) {
                detectWords.add(schemaElementMatch.getDetectWord());
            }
        }
        for (SchemaElement schemaItem : semanticParseInfo.getMetrics()) {
            detectWords.add(
                    detectWordMap.getOrDefault(Optional.ofNullable(schemaItem.getId()).orElse(0L), ""));
            // only first metric
            break;
        }
        for (SchemaElement schemaItem : semanticParseInfo.getDimensions()) {
            detectWords.add(
                    detectWordMap.getOrDefault(Optional.ofNullable(schemaItem.getId()).orElse(0L), ""));
            // only first dimension
            break;
        }
        String dateText = Optional.ofNullable(semanticParseInfo.getDateInfo()).orElse(new DateConf()).getText();
        if (StringUtils.isNotBlank(dateText) && !dateText.equalsIgnoreCase(Constants.NULL)) {
            detectWords.add(dateText);
        }
        detectWords.removeIf(word -> !text.contains(word) && !text.contains(StringUtils.reverse(word)));
        //compare the length between detect words and query text
        return checkThreshold(text, detectWords, semanticParseInfo);
    }

    private static boolean checkThreshold(String queryText, List<String> detectWords, SemanticParseInfo semanticParseInfo) {
        String detectWordsDistinct = StringUtils.join(new HashSet<>(detectWords), "");
        int detectWordsLength = detectWordsDistinct.length();
        int queryTextLength = queryText.length();
        double degree = detectWordsLength * 1.0 / queryTextLength;
        if (queryTextLength > QUERY_TEXT_LENGTH_THRESHOLD) {
            if (degree < LONG_TEXT_THRESHOLD) {
                return false;
            }
        } else if (degree < SHORT_TEXT_THRESHOLD) {
            return false;
        }
        log.info("queryMode:{}, degree:{}, detectWords:{}, parse info:{}",
                semanticParseInfo.getQueryMode(), degree, detectWordsDistinct, semanticParseInfo);
        return true;
    }

}
