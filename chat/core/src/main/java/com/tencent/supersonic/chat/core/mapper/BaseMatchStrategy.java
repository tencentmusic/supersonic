package com.tencent.supersonic.chat.core.mapper;

import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.core.knowledge.helper.NatureHelper;
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
    private MapperHelper mapperHelper;

    @Override
    public Map<MatchText, List<T>> match(QueryContext queryContext, List<S2Term> terms,
                                         Set<Long> detectViewIds) {
        String text = queryContext.getQueryText();
        if (Objects.isNull(terms) || StringUtils.isEmpty(text)) {
            return null;
        }

        log.debug("terms:{},,detectViewIds:{}", terms, detectViewIds);

        List<T> detects = detect(queryContext, terms, detectViewIds);
        Map<MatchText, List<T>> result = new HashMap<>();

        result.put(MatchText.builder().regText(text).detectSegment(text).build(), detects);
        return result;
    }

    public List<T> detect(QueryContext queryContext, List<S2Term> terms, Set<Long> detectModelIds) {
        Map<Integer, Integer> regOffsetToLength = getRegOffsetToLength(terms);
        String text = queryContext.getQueryText();
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
        Set<Long> viewIds = mapperHelper.getViewIds(queryContext.getViewId(), queryContext.getAgent());
        terms = filterByViewId(terms, viewIds);
        Map<MatchText, List<T>> matchResult = match(queryContext, terms, viewIds);
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

    public List<S2Term> filterByViewId(List<S2Term> terms, Set<Long> viewIds) {
        logTerms(terms);
        if (CollectionUtils.isNotEmpty(viewIds)) {
            terms = terms.stream().filter(term -> {
                Long viewId = NatureHelper.getViewId(term.getNature().toString());
                if (Objects.nonNull(viewId)) {
                    return viewIds.contains(viewId);
                }
                return false;
            }).collect(Collectors.toList());
            log.info("terms filter by viewId:{}", viewIds);
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

    public abstract void detectByStep(QueryContext queryContext, Set<T> results,
            Set<Long> detectModelIds, Integer startIndex, Integer index, int offset);

}
