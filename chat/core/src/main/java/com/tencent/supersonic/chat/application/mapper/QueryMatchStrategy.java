package com.tencent.supersonic.chat.application.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.domain.pojo.search.MatchText;
import com.tencent.supersonic.common.nlp.MapResult;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.knowledge.infrastructure.nlp.Suggester;
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

    @Override
    public Map<MatchText, List<MapResult>> match(String text, List<Term> terms, Integer detectDomainId) {
        if (CollectionUtils.isEmpty(terms) || StringUtils.isEmpty(text)) {
            return null;
        }
        Map<Integer, Integer> regOffsetToLength = terms.stream().sorted(Comparator.comparing(Term::length))
                .collect(Collectors.toMap(Term::getOffset, term -> term.word.length(), (value1, value2) -> value2));

        List<Integer> offsetList = terms.stream().sorted(Comparator.comparing(Term::getOffset))
                .map(term -> term.getOffset()).collect(Collectors.toList());

        log.debug("retryCount:{},terms:{},regOffsetToLength:{},offsetList:{},detectDomainId:{}", terms,
                regOffsetToLength, offsetList, detectDomainId);

        List<MapResult> detects = detect(text, regOffsetToLength, offsetList, detectDomainId);
        Map<MatchText, List<MapResult>> result = new HashMap<>();
        MatchText matchText = new MatchText(text, text);
        result.put(matchText, detects);
        return result;
    }

    private List<MapResult> detect(String text, Map<Integer, Integer> regOffsetToLength, List<Integer> offsetList,
            Integer detectDomainId) {
        List<MapResult> results = Lists.newArrayList();

        for (Integer index = 0; index <= text.length() - 1; ) {

            Set<MapResult> mapResultRowSet = new LinkedHashSet();

            for (Integer i = index; i <= text.length(); ) {
                int offset = mapperHelper.getStepOffset(offsetList, index);
                i = mapperHelper.getStepIndex(regOffsetToLength, i);
                if (i <= text.length()) {
                    List<MapResult> mapResults = detectByStep(text, detectDomainId, index, i, offset);
                    mapResultRowSet.addAll(mapResults);
                }
            }

            index = mapperHelper.getStepIndex(regOffsetToLength, index);
            results.addAll(mapResultRowSet);
        }
        return results;
    }

    private List<MapResult> detectByStep(String text, Integer detectDomainId, Integer index, Integer i, int offset) {
        String detectSegment = text.substring(index, i);
        Integer oneDetectionSize = mapperHelper.getOneDetectionSize();
        // step1. pre search
        LinkedHashSet<MapResult> mapResults = Suggester.prefixSearch(detectSegment,
                        mapperHelper.getOneDetectionMaxSize())
                .stream().collect(Collectors.toCollection(LinkedHashSet::new));
        // step2. suffix search
        LinkedHashSet<MapResult> suffixMapResults = Suggester.suffixSearch(detectSegment, oneDetectionSize)
                .stream().collect(Collectors.toCollection(LinkedHashSet::new));

        mapResults.addAll(suffixMapResults);

        if (CollectionUtils.isEmpty(mapResults)) {
            return new ArrayList<>();
        }
        // step3. merge pre/suffix result
        mapResults = mapResults.stream().sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // step4. filter by classId
        if (Objects.nonNull(detectDomainId) && detectDomainId > 0) {
            log.debug("detectDomainId:{}, before parseResults:{}", mapResults);
            mapResults = mapResults.stream().map(entry -> {
                List<String> natures = entry.getNatures().stream().filter(
                        nature -> nature.startsWith(NatureType.NATURE_SPILT + detectDomainId) || (nature.startsWith(
                                NatureType.NATURE_SPILT))
                ).collect(Collectors.toList());
                entry.setNatures(natures);
                return entry;
            }).collect(Collectors.toCollection(LinkedHashSet::new));
            log.info("after domainId parseResults:{}", mapResults);
        }
        // step5. filter by similarity
        mapResults = mapResults.stream()
                .filter(term -> mapperHelper.getSimilarity(detectSegment, term.getName())
                        >= mapperHelper.getThresholdMatch(term.getNatures()))
                .filter(term -> CollectionUtils.isNotEmpty(term.getNatures()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.debug("metricDimensionThreshold:{},dimensionValueThreshold:{},after isSimilarity  parseResults:{}",
                mapResults);

        mapResults = mapResults.stream().map(parseResult -> {
            parseResult.setOffset(offset);
            parseResult.setSimilarity(mapperHelper.getSimilarity(detectSegment, parseResult.getName()));
            return parseResult;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        // step6. take only one dimension or 10 metric/dimension value per rond.
        List<MapResult> dimensionMetrics = mapResults.stream()
                .filter(entry -> mapperHelper.existDimensionValues(entry.getNatures()))
                .collect(Collectors.toList())
                .stream()
                .limit(1)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(dimensionMetrics)) {
            return dimensionMetrics;
        } else {
            return mapResults.stream().limit(oneDetectionSize).collect(Collectors.toList());
        }
    }
}