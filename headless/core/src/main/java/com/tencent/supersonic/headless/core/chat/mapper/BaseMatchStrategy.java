package com.tencent.supersonic.headless.core.chat.mapper;


import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.core.config.MapperConfig;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.chat.knowledge.helper.NatureHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public abstract class BaseMatchStrategy<T> implements MatchStrategy<T> {

    @Autowired
    protected MapperHelper mapperHelper;

    @Autowired
    protected MapperConfig mapperConfig;

    @Override
    public Map<MatchText, List<T>> match(QueryContext queryContext, List<S2Term> terms,
                                         Set<Long> detectDataSetIds) {
        String text = queryContext.getQueryText();
        if (Objects.isNull(terms) || StringUtils.isEmpty(text)) {
            return null;
        }

        log.debug("terms:{},,detectDataSetIds:{}", terms, detectDataSetIds);

        List<T> detects = detect(queryContext, terms, detectDataSetIds);
        Map<MatchText, List<T>> result = new HashMap<>();

        result.put(MatchText.builder().regText(text).detectSegment(text).build(), detects);
        return result;
    }

    public List<T> detect(QueryContext queryContext, List<S2Term> terms, Set<Long> detectDataSetIds) {
        Map<Integer, Integer> regOffsetToLength = getRegOffsetToLength(terms);
        String text = queryContext.getQueryText();
        Set<T> results = new HashSet<>();

        Set<String> detectSegments = new HashSet<>();

        for (Integer startIndex = 0; startIndex <= text.length() - 1; ) {

            for (Integer index = startIndex; index <= text.length(); ) {
                int offset = mapperHelper.getStepOffset(terms, startIndex);
                index = mapperHelper.getStepIndex(regOffsetToLength, index);
                if (index <= text.length()) {
                    String detectSegment = text.substring(startIndex, index).trim();
                    detectSegments.add(detectSegment);
                    detectByStep(queryContext, results, detectDataSetIds, detectSegment, offset);
                }
            }
            startIndex = mapperHelper.getStepIndex(regOffsetToLength, startIndex);
        }
        detectByBatch(queryContext, results, detectDataSetIds, detectSegments);
        return new ArrayList<>(results);
    }

    protected void detectByBatch(QueryContext queryContext, Set<T> results, Set<Long> detectDataSetIds,
                                 Set<String> detectSegments) {
    }

    public Map<Integer, Integer> getRegOffsetToLength(List<S2Term> terms) {
        return terms.stream().sorted(Comparator.comparing(S2Term::length))
                .collect(Collectors.toMap(S2Term::getOffset, term -> term.word.length(),
                        (value1, value2) -> value2));
    }

    public void selectResultInOneRound(Set<T> existResults, List<T> oneRoundResults) {
        if (CollectionUtils.isEmpty(oneRoundResults)) {
            return;
        }
        for (T oneRoundResult : oneRoundResults) {
            if (existResults.contains(oneRoundResult)) {
                boolean isDeleted = existResults.removeIf(
                        existResult -> {
                            boolean delete = needDelete(oneRoundResult, existResult);
                            if (delete) {
                                log.info("deleted existResult:{}", existResult);
                            }
                            return delete;
                        }
                );
                if (isDeleted) {
                    log.info("deleted, add oneRoundResult:{}", oneRoundResult);
                    existResults.add(oneRoundResult);
                }
            } else {
                existResults.add(oneRoundResult);
            }
        }
    }

    public List<T> getMatches(QueryContext queryContext, List<S2Term> terms) {
        Set<Long> dataSetIds = queryContext.getDataSetIds();
        terms = filterByDataSetId(terms, dataSetIds);
        Map<MatchText, List<T>> matchResult = match(queryContext, terms, dataSetIds);
        List<T> matches = new ArrayList<>();
        if (Objects.isNull(matchResult)) {
            return matches;
        }
        Optional<List<T>> first = matchResult.entrySet().stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .map(entry -> entry.getValue()).findFirst();

        if (first.isPresent()) {
            matches = first.get();
        }
        return matches;
    }

    public List<S2Term> filterByDataSetId(List<S2Term> terms, Set<Long> dataSetIds) {
        logTerms(terms);
        if (CollectionUtils.isNotEmpty(dataSetIds)) {
            terms = terms.stream().filter(term -> {
                Long dataSetId = NatureHelper.getDataSetId(term.getNature().toString());
                if (Objects.nonNull(dataSetId)) {
                    return dataSetIds.contains(dataSetId);
                }
                return false;
            }).collect(Collectors.toList());
            log.info("terms filter by dataSetId:{}", dataSetIds);
            logTerms(terms);
        }
        return terms;
    }

    public void logTerms(List<S2Term> terms) {
        if (CollectionUtils.isEmpty(terms)) {
            return;
        }
        for (S2Term term : terms) {
            log.debug("word:{},nature:{},frequency:{}", term.word, term.nature.toString(), term.getFrequency());
        }
    }

    public abstract boolean needDelete(T oneRoundResult, T existResult);

    public abstract String getMapKey(T a);

    public abstract void detectByStep(QueryContext queryContext, Set<T> existResults, Set<Long> detectDataSetIds,
                                      String detectSegment, int offset);

    public double getThreshold(Double threshold, Double minThreshold, MapModeEnum mapModeEnum) {
        double decreaseAmount = (threshold - minThreshold) / 4;
        double divideThreshold = threshold - mapModeEnum.threshold * decreaseAmount;
        return divideThreshold >= minThreshold ? divideThreshold : minThreshold;
    }
}
