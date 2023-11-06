package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.knowledge.dictionary.HanlpMapResult;
import com.tencent.supersonic.knowledge.service.SearchService;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * match strategy implement
 */
@Service
@Slf4j
public class HanlpDictMatchStrategy extends BaseMatchStrategy<HanlpMapResult> {

    @Autowired
    private MapperHelper mapperHelper;

    @Autowired
    private OptimizationConfig optimizationConfig;

    @Override
    public Map<MatchText, List<HanlpMapResult>> match(QueryContext queryContext, List<Term> terms,
            Set<Long> detectModelIds) {
        QueryReq queryReq = queryContext.getRequest();
        String text = queryReq.getQueryText();
        if (Objects.isNull(terms) || StringUtils.isEmpty(text)) {
            return null;
        }

        log.debug("retryCount:{},terms:{},,detectModelIds:{}", terms, detectModelIds);

        List<HanlpMapResult> detects = detect(queryContext, terms, detectModelIds);
        Map<MatchText, List<HanlpMapResult>> result = new HashMap<>();

        result.put(MatchText.builder().regText(text).detectSegment(text).build(), detects);
        return result;
    }

    @Override
    public boolean needDelete(HanlpMapResult oneRoundResult, HanlpMapResult existResult) {
        return getMapKey(oneRoundResult).equals(getMapKey(existResult))
                && existResult.getDetectWord().length() < oneRoundResult.getDetectWord().length();
    }

    public void detectByStep(QueryContext queryContext, Set<HanlpMapResult> existResults, Set<Long> detectModelIds,
            Integer startIndex, Integer index, int offset) {
        QueryReq queryReq = queryContext.getRequest();
        String text = queryReq.getQueryText();
        Integer agentId = queryReq.getAgentId();
        String detectSegment = text.substring(startIndex, index);

        // step1. pre search
        Integer oneDetectionMaxSize = optimizationConfig.getOneDetectionMaxSize();
        LinkedHashSet<HanlpMapResult> hanlpMapResults = SearchService.prefixSearch(detectSegment, oneDetectionMaxSize,
                agentId,
                detectModelIds).stream().collect(Collectors.toCollection(LinkedHashSet::new));
        // step2. suffix search
        LinkedHashSet<HanlpMapResult> suffixHanlpMapResults = SearchService.suffixSearch(detectSegment,
                        oneDetectionMaxSize, agentId, detectModelIds).stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        hanlpMapResults.addAll(suffixHanlpMapResults);

        if (CollectionUtils.isEmpty(hanlpMapResults)) {
            return;
        }
        // step3. merge pre/suffix result
        hanlpMapResults = hanlpMapResults.stream().sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // step4. filter by similarity
        hanlpMapResults = hanlpMapResults.stream()
                .filter(term -> mapperHelper.getSimilarity(detectSegment, term.getName())
                        >= mapperHelper.getThresholdMatch(term.getNatures()))
                .filter(term -> CollectionUtils.isNotEmpty(term.getNatures()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("after isSimilarity parseResults:{}", hanlpMapResults);

        hanlpMapResults = hanlpMapResults.stream().map(parseResult -> {
            parseResult.setOffset(offset);
            parseResult.setSimilarity(mapperHelper.getSimilarity(detectSegment, parseResult.getName()));
            return parseResult;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        // step5. take only one dimension or 10 metric/dimension value per rond.
        List<HanlpMapResult> dimensionMetrics = hanlpMapResults.stream()
                .filter(entry -> mapperHelper.existDimensionValues(entry.getNatures()))
                .collect(Collectors.toList())
                .stream()
                .limit(1)
                .collect(Collectors.toList());

        Integer oneDetectionSize = optimizationConfig.getOneDetectionSize();
        List<HanlpMapResult> oneRoundResults = hanlpMapResults.stream().limit(oneDetectionSize)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(dimensionMetrics)) {
            oneRoundResults = dimensionMetrics;
        }
        // step6. select mapResul in one round
        selectResultInOneRound(existResults, oneRoundResults);
    }

    public String getMapKey(HanlpMapResult a) {
        return a.getName() + Constants.UNDERLINE + String.join(Constants.UNDERLINE, a.getNatures());
    }
}
