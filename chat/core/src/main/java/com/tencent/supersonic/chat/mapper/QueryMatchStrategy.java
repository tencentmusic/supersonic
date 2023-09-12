package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import com.tencent.supersonic.knowledge.service.SearchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * match strategy implement
 */
@Service
@Slf4j
public class QueryMatchStrategy implements MatchStrategy {

    @Autowired
    private MapperHelper mapperHelper;

    @Autowired
    private OptimizationConfig optimizationConfig;

    @Override
    public Map<MatchText, List<MapResult>> match(QueryReq queryReq, List<Term> terms, Set<Long> detectModelIds) {
        String text = queryReq.getQueryText();
        if (Objects.isNull(terms) || StringUtils.isEmpty(text)) {
            return null;
        }

        Map<Integer, Integer> regOffsetToLength = terms.stream().sorted(Comparator.comparing(Term::length))
                .collect(Collectors.toMap(Term::getOffset, term -> term.word.length(), (value1, value2) -> value2));

        List<Integer> offsetList = terms.stream().sorted(Comparator.comparing(Term::getOffset))
                .map(term -> term.getOffset()).collect(Collectors.toList());

        log.debug("retryCount:{},terms:{},regOffsetToLength:{},offsetList:{},detectModelIds:{}", terms,
                regOffsetToLength, offsetList, detectModelIds);

        List<MapResult> detects = detect(queryReq, regOffsetToLength, offsetList, detectModelIds);
        Map<MatchText, List<MapResult>> result = new HashMap<>();

        result.put(MatchText.builder().regText(text).detectSegment(text).build(), detects);
        return result;
    }

    private List<MapResult> detect(QueryReq queryReq, Map<Integer, Integer> regOffsetToLength, List<Integer> offsetList,
            Set<Long> detectModelIds) {
        String text = queryReq.getQueryText();
        List<MapResult> results = Lists.newArrayList();

        for (Integer index = 0; index <= text.length() - 1; ) {

            Set<MapResult> mapResultRowSet = new LinkedHashSet();

            for (Integer i = index; i <= text.length(); ) {
                int offset = mapperHelper.getStepOffset(offsetList, index);
                i = mapperHelper.getStepIndex(regOffsetToLength, i);
                if (i <= text.length()) {
                    List<MapResult> mapResults = detectByStep(queryReq, detectModelIds, index, i, offset);
                    selectMapResultInOneRound(mapResultRowSet, mapResults);
                }
            }
            index = mapperHelper.getStepIndex(regOffsetToLength, index);
            results.addAll(mapResultRowSet);
        }
        return results;
    }

    private void selectMapResultInOneRound(Set<MapResult> mapResultRowSet, List<MapResult> mapResults) {
        for (MapResult mapResult : mapResults) {
            if (mapResultRowSet.contains(mapResult)) {
                boolean isDeleted = mapResultRowSet.removeIf(
                        entry -> {
                            boolean deleted = getMapKey(mapResult).equals(getMapKey(entry))
                                    && entry.getDetectWord().length() < mapResult.getDetectWord().length();
                            if (deleted) {
                                log.info("deleted entry:{}", entry);
                            }
                            return deleted;
                        }
                );
                if (isDeleted) {
                    log.info("deleted, add mapResult:{}", mapResult);
                    mapResultRowSet.add(mapResult);
                }
            } else {
                mapResultRowSet.add(mapResult);
            }
        }
    }

    private String getMapKey(MapResult a) {
        return a.getName() + Constants.UNDERLINE + String.join(Constants.UNDERLINE, a.getNatures());
    }

    private List<MapResult> detectByStep(QueryReq queryReq, Set<Long> detectModelIds, Integer index, Integer i,
            int offset) {
        String text = queryReq.getQueryText();
        Integer agentId = queryReq.getAgentId();
        String detectSegment = text.substring(index, i);

        // step1. pre search
        Integer oneDetectionMaxSize = optimizationConfig.getOneDetectionMaxSize();
        LinkedHashSet<MapResult> mapResults = SearchService.prefixSearch(detectSegment, oneDetectionMaxSize, agentId,
                detectModelIds).stream().collect(Collectors.toCollection(LinkedHashSet::new));
        // step2. suffix search
        LinkedHashSet<MapResult> suffixMapResults = SearchService.suffixSearch(detectSegment, oneDetectionMaxSize,
                agentId, detectModelIds).stream().collect(Collectors.toCollection(LinkedHashSet::new));

        mapResults.addAll(suffixMapResults);

        if (CollectionUtils.isEmpty(mapResults)) {
            return new ArrayList<>();
        }
        // step3. merge pre/suffix result
        mapResults = mapResults.stream().sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // step4. filter by similarity
        mapResults = mapResults.stream()
                .filter(term -> mapperHelper.getSimilarity(detectSegment, term.getName())
                        >= mapperHelper.getThresholdMatch(term.getNatures()))
                .filter(term -> CollectionUtils.isNotEmpty(term.getNatures()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("after isSimilarity parseResults:{}", mapResults);

        mapResults = mapResults.stream().map(parseResult -> {
            parseResult.setOffset(offset);
            parseResult.setSimilarity(mapperHelper.getSimilarity(detectSegment, parseResult.getName()));
            return parseResult;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        // step5. take only one dimension or 10 metric/dimension value per rond.
        List<MapResult> dimensionMetrics = mapResults.stream()
                .filter(entry -> mapperHelper.existDimensionValues(entry.getNatures()))
                .collect(Collectors.toList())
                .stream()
                .limit(1)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(dimensionMetrics)) {
            return dimensionMetrics;
        } else {
            return mapResults.stream().limit(optimizationConfig.getOneDetectionSize()).collect(Collectors.toList());
        }
    }
}
