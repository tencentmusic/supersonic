package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
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
 * match strategy implement
 */
@Service
@Slf4j
public abstract class BaseMatchStrategy<T> implements MatchStrategy<T> {

    @Autowired
    private MapperHelper mapperHelper;


    @Override
    public Map<MatchText, List<T>> match(QueryReq queryReq, List<Term> terms, Set<Long> detectModelIds) {
        String text = queryReq.getQueryText();
        if (Objects.isNull(terms) || StringUtils.isEmpty(text)) {
            return null;
        }

        log.debug("retryCount:{},terms:{},,detectModelIds:{}", terms, detectModelIds);

        List<T> detects = detect(queryReq, terms, detectModelIds);
        Map<MatchText, List<T>> result = new HashMap<>();

        result.put(MatchText.builder().regText(text).detectSegment(text).build(), detects);
        return result;
    }

    public List<T> detect(QueryReq queryReq, List<Term> terms, Set<Long> detectModelIds) {
        Map<Integer, Integer> regOffsetToLength = getRegOffsetToLength(terms);
        String text = queryReq.getQueryText();
        Set<T> results = new HashSet<>();

        for (Integer index = 0; index <= text.length() - 1; ) {

            for (Integer i = index; i <= text.length(); ) {
                int offset = mapperHelper.getStepOffset(terms, index);
                i = mapperHelper.getStepIndex(regOffsetToLength, i);
                if (i <= text.length()) {
                    detectByStep(queryReq, results, detectModelIds, index, i, offset);
                }
            }
            index = mapperHelper.getStepIndex(regOffsetToLength, index);
        }
        return new ArrayList<>(results);
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

    public List<T> getMatches(QueryReq queryReq, List<Term> terms, Set<Long> detectModelIds) {
        Map<MatchText, List<T>> matchResult = match(queryReq, terms, detectModelIds);
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

    public abstract boolean needDelete(T oneRoundResult, T existResult);

    public abstract String getMapKey(T a);

    public abstract void detectByStep(QueryReq queryReq, Set<T> results, Set<Long> detectModelIds, Integer startIndex,
            Integer index, int offset);


}
