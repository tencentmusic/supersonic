package com.tencent.supersonic.chat.application.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.domain.pojo.search.MatchText;
import com.tencent.supersonic.common.nlp.MapResult;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.knowledge.infrastructure.nlp.Suggester;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * match strategy implement
 */
@Service
public class QueryMatchStrategy implements MatchStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryMatchStrategy.class);
    public static final double STEP = 0.1;
    @Value("${one.detection.size:6}")
    private Integer oneDetectionSize;
    @Value("${one.detection.max.size:20}")
    private Integer oneDetectionMaxSize;
    @Value("${metric.dimension.threshold:0.3}")
    private Double metricDimensionThresholdConfig;
    @Value("${dimension.value.threshold:0.5}")
    private Double dimensionValueThresholdConfig;

    @Override
    public List<MapResult> match(String text, List<Term> terms, int retryCount) {
        return match(text, terms, retryCount, null);
    }

    @Override
    public List<MapResult> match(String text, List<Term> terms, int retryCount, Integer detectDomainId) {
        if (CollectionUtils.isEmpty(terms) || StringUtils.isEmpty(text)) {
            return null;
        }
        Map<Integer, Integer> regOffsetToLength = terms.stream().sorted(Comparator.comparing(Term::length))
                .collect(Collectors.toMap(Term::getOffset, term -> term.word.length(), (value1, value2) -> value2));
        List<Integer> offsetList = terms.stream().sorted(Comparator.comparing(Term::getOffset))
                .map(term -> term.getOffset()).collect(Collectors.toList());

        LOGGER.debug("retryCount:{},terms:{},regOffsetToLength:{},offsetList:{},detectDomainId:{}", retryCount, terms,
                regOffsetToLength, offsetList,
                detectDomainId);

        return detect(text, regOffsetToLength, offsetList, detectDomainId, retryCount);
    }

    @Override
    public Map<MatchText, List<MapResult>> matchWithMatchText(String text, List<Term> originals) {

        return null;
    }

    private List<MapResult> detect(String text, Map<Integer, Integer> regOffsetToLength, List<Integer> offsetList,
            Integer detectDomainId, int retryCount) {
        List<MapResult> results = Lists.newArrayList();

        for (Integer index = 0; index <= text.length() - 1; ) {

            Set<MapResult> mapResultRowSet = new LinkedHashSet();

            for (Integer i = index; i <= text.length(); ) {
                int offset = getStepOffset(offsetList, index);
                i = getStepIndex(regOffsetToLength, i);
                if (i <= text.length()) {
                    List<MapResult> mapResults = detectByStep(text, detectDomainId, index, i, offset, retryCount);
                    mapResultRowSet.addAll(mapResults);
                }
            }

            index = getStepIndex(regOffsetToLength, index);
            results.addAll(mapResultRowSet);
        }
        return results;
    }

    private List<MapResult> detectByStep(String text, Integer detectClassId, Integer index, Integer i, int offset,
            int retryCount) {
        String detectSegment = text.substring(index, i);
        // step1. pre search
        LinkedHashSet<MapResult> mapResults = Suggester.prefixSearch(detectSegment, oneDetectionMaxSize)
                .stream().collect(Collectors.toCollection(LinkedHashSet::new));
        // step2. suffix search
        LinkedHashSet<MapResult> suffixMapResults = Suggester.suffixSearch(detectSegment, oneDetectionMaxSize)
                .stream().collect(Collectors.toCollection(LinkedHashSet::new));

        mapResults.addAll(suffixMapResults);

        if (CollectionUtils.isEmpty(mapResults)) {
            return new ArrayList<>();
        }
        // step3. merge pre/suffix result
        mapResults = mapResults.stream().sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // step4. filter by classId
        if (Objects.nonNull(detectClassId) && detectClassId > 0) {
            LOGGER.debug("detectDomainId:{}, before parseResults:{}", mapResults);
            mapResults = mapResults.stream().map(entry -> {
                List<String> natures = entry.getNatures().stream().filter(
                        nature -> nature.startsWith(NatureType.NATURE_SPILT + detectClassId) || (nature.startsWith(
                                NatureType.NATURE_SPILT))
                ).collect(Collectors.toList());
                entry.setNatures(natures);
                return entry;
            }).collect(Collectors.toCollection(LinkedHashSet::new));
            LOGGER.info("after domainId parseResults:{}", mapResults);
        }
        // step5. filter by similarity
        mapResults = mapResults.stream()
                .filter(term -> getSimilarity(detectSegment, term.getName()) >= getThresholdMatch(term.getNatures(),
                        retryCount))
                .filter(term -> CollectionUtils.isNotEmpty(term.getNatures()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        LOGGER.debug("metricDimensionThreshold:{},dimensionValueThreshold:{},after isSimilarity  parseResults:{}",
                mapResults);

        mapResults = mapResults.stream().map(parseResult -> {
            parseResult.setOffset(offset);
            parseResult.setSimilarity(getSimilarity(detectSegment, parseResult.getName()));
            return parseResult;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        // step6. take only one dimension or 10 metric/dimension value per rond.
        List<MapResult> dimensionMetrics = mapResults.stream().filter(entry -> existDimensionValues(entry.getNatures()))
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

    private Integer getStepIndex(Map<Integer, Integer> regOffsetToLength, Integer index) {
        Integer subRegLength = regOffsetToLength.get(index);
        if (Objects.nonNull(subRegLength)) {
            index = index + subRegLength;
        } else {
            index++;
        }
        return index;
    }

    private Integer getStepOffset(List<Integer> termList, Integer index) {
        for (int j = 0; j < termList.size() - 1; j++) {
            if (termList.get(j) <= index && termList.get(j + 1) > index) {
                return termList.get(j);
            }
        }
        return index;
    }

    private double getThresholdMatch(List<String> natures, int retryCount) {
        if (existDimensionValues(natures)) {
            return dimensionValueThresholdConfig;
        }
        return metricDimensionThresholdConfig - STEP * retryCount;
    }

}