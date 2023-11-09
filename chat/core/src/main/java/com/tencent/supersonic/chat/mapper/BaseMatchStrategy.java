package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.knowledge.utils.NatureHelper;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Base Match Strategy
 */
@Service
@Slf4j
public abstract class BaseMatchStrategy<T> implements MatchStrategy<T> {

    @Autowired
    private MapperHelper mapperHelper;

    @Override
    public Map<MatchText, List<T>> match(QueryContext queryContext, List<Term> terms, Set<Long> detectModelIds) {
        String text = queryContext.getRequest().getQueryText();
        if (Objects.isNull(terms) || StringUtils.isEmpty(text)) {
            return null;
        }

        log.debug("terms:{},,detectModelIds:{}", terms, detectModelIds);

        List<T> detects = detect(queryContext, terms, detectModelIds);
        Map<MatchText, List<T>> result = new HashMap<>();

        result.put(MatchText.builder().regText(text).detectSegment(text).build(), detects);
        return result;
    }

    public List<T> detect(QueryContext queryContext, List<Term> terms, Set<Long> detectModelIds) {
        Map<Integer, Integer> regOffsetToLength = getRegOffsetToLength(terms);
        String text = queryContext.getRequest().getQueryText();
        Set<T> results = new HashSet<>();

        Set<String> detectSegments = new HashSet<>();

        for (Integer startIndex = 0; startIndex <= text.length() - 1; ) {

            for (Integer index = startIndex; index <= text.length(); ) {
                int offset = mapperHelper.getStepOffset(terms, startIndex);
                index = mapperHelper.getStepIndex(regOffsetToLength, index);
                if (index <= text.length()) {
                    String detectSegment = text.substring(startIndex, index);
                    detectSegments.add(detectSegment);
                    detectByStep(queryContext, results, detectModelIds, startIndex, index, offset);
                }
            }
            startIndex = mapperHelper.getStepIndex(regOffsetToLength, startIndex);
        }
        detectByBatch(queryContext, results, detectModelIds, detectSegments);
        return new ArrayList<>(results);
    }

    protected void detectByBatch(QueryContext queryContext, Set<T> results, Set<Long> detectModelIds,
            Set<String> detectSegments) {
        return;
    }

    public Map<Integer, Integer> getRegOffsetToLength(List<Term> terms) {
        return terms.stream().sorted(Comparator.comparing(Term::length))
                .collect(Collectors.toMap(Term::getOffset, term -> term.word.length(), (value1, value2) -> value2));
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

    public List<T> getMatches(QueryContext queryContext, List<Term> terms) {
        Set<Long> detectModelIds = mapperHelper.getModelIds(queryContext.getRequest());
        terms = filterByModelIds(terms, detectModelIds);
        Map<MatchText, List<T>> matchResult = match(queryContext, terms, detectModelIds);
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

    public List<Term> filterByModelIds(List<Term> terms, Set<Long> detectModelIds) {
        logTerms(terms);
        if (CollectionUtils.isNotEmpty(detectModelIds)) {
            terms = terms.stream().filter(term -> {
                Long modelId = NatureHelper.getModelId(term.getNature().toString());
                if (Objects.nonNull(modelId)) {
                    return detectModelIds.contains(modelId);
                }
                return false;
            }).collect(Collectors.toList());
            log.info("terms filter by modelIds:{}", detectModelIds);
            logTerms(terms);
        }
        return terms;
    }

    public void logTerms(List<Term> terms) {
        if (CollectionUtils.isEmpty(terms)) {
            return;
        }
        for (Term term : terms) {
            log.debug("word:{},nature:{},frequency:{}", term.word, term.nature.toString(), term.getFrequency());
        }
    }

    public abstract boolean needDelete(T oneRoundResult, T existResult);

    public abstract String getMapKey(T a);

    public abstract void detectByStep(QueryContext queryContext, Set<T> results,
            Set<Long> detectModelIds, Integer startIndex, Integer index, int offset);


}
