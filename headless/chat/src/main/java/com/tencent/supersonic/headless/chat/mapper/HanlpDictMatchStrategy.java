package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.MAPPER_DETECTION_MAX_SIZE;
import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.MAPPER_DETECTION_SIZE;
import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.MAPPER_DIMENSION_VALUE_SIZE;

/**
 * HanlpDictMatchStrategy uses <a href="https://www.hanlp.com/">HanLP</a> to
 * match schema elements. It currently supports prefix and suffix matching
 * against names, values and aliases.
 */
@Service
@Slf4j
public class HanlpDictMatchStrategy extends BaseMatchStrategy<HanlpMapResult> {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public Map<MatchText, List<HanlpMapResult>> match(QueryContext queryContext, List<S2Term> terms,
                                                      Set<Long> detectDataSetIds) {
        String text = queryContext.getQueryText();
        if (Objects.isNull(terms) || StringUtils.isEmpty(text)) {
            return null;
        }

        log.debug("terms:{},detectModelIds:{}", terms, detectDataSetIds);

        List<HanlpMapResult> detects = detect(queryContext, terms, detectDataSetIds);
        Map<MatchText, List<HanlpMapResult>> result = new HashMap<>();

        result.put(MatchText.builder().regText(text).detectSegment(text).build(), detects);
        return result;
    }

    @Override
    public boolean needDelete(HanlpMapResult oneRoundResult, HanlpMapResult existResult) {
        return getMapKey(oneRoundResult).equals(getMapKey(existResult))
                && existResult.getDetectWord().length() < oneRoundResult.getDetectWord().length();
    }

    public void detectByStep(QueryContext queryContext, Set<HanlpMapResult> existResults, Set<Long> detectDataSetIds,
            String detectSegment, int offset) {
        // step1. pre search
        Integer oneDetectionMaxSize = Integer.valueOf(mapperConfig.getParameterValue(MAPPER_DETECTION_MAX_SIZE));
        LinkedHashSet<HanlpMapResult> hanlpMapResults = knowledgeBaseService.prefixSearch(detectSegment,
                        oneDetectionMaxSize, queryContext.getModelIdToDataSetIds(), detectDataSetIds)
                .stream().collect(Collectors.toCollection(LinkedHashSet::new));
        // step2. suffix search
        LinkedHashSet<HanlpMapResult> suffixHanlpMapResults = knowledgeBaseService.suffixSearch(detectSegment,
                        oneDetectionMaxSize, queryContext.getModelIdToDataSetIds(), detectDataSetIds)
                .stream().collect(Collectors.toCollection(LinkedHashSet::new));

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
                        >= getThresholdMatch(term.getNatures(), queryContext))
                .filter(term -> CollectionUtils.isNotEmpty(term.getNatures()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.debug("detectSegment:{},after isSimilarity parseResults:{}", detectSegment, hanlpMapResults);

        hanlpMapResults = hanlpMapResults.stream().map(parseResult -> {
            parseResult.setOffset(offset);
            parseResult.setSimilarity(mapperHelper.getSimilarity(detectSegment, parseResult.getName()));
            return parseResult;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        // step5. take only M dimensionValue or N-M metric/dimension value per rond.
        int oneDetectionValueSize = Integer.valueOf(mapperConfig.getParameterValue(MAPPER_DIMENSION_VALUE_SIZE));
        List<HanlpMapResult> dimensionValues = hanlpMapResults.stream()
                .filter(entry -> mapperHelper.existDimensionValues(entry.getNatures()))
                .limit(oneDetectionValueSize)
                .collect(Collectors.toList());

        Integer oneDetectionSize = Integer.valueOf(mapperConfig.getParameterValue(MAPPER_DETECTION_SIZE));
        List<HanlpMapResult> oneRoundResults = new ArrayList<>();

        // add the dimensionValue if it exists
        if (CollectionUtils.isNotEmpty(dimensionValues)) {
            oneRoundResults.addAll(dimensionValues);
        }
        // fill the rest of the list with other results, excluding the dimensionValue if it was added
        if (oneRoundResults.size() < oneDetectionSize) {
            List<HanlpMapResult> additionalResults = hanlpMapResults.stream()
                    .filter(entry -> !mapperHelper.existDimensionValues(entry.getNatures())
                            && !oneRoundResults.contains(entry))
                    .limit(oneDetectionSize - oneRoundResults.size())
                    .collect(Collectors.toList());
            oneRoundResults.addAll(additionalResults);
        }
        // step6. select mapResul in one round
        selectResultInOneRound(existResults, oneRoundResults);
    }

    public String getMapKey(HanlpMapResult a) {
        return a.getName() + Constants.UNDERLINE + String.join(Constants.UNDERLINE, a.getNatures());
    }

    public double getThresholdMatch(List<String> natures, QueryContext queryContext) {
        Double threshold = Double.valueOf(mapperConfig.getParameterValue(MapperConfig.MAPPER_NAME_THRESHOLD));
        Double minThreshold = Double.valueOf(mapperConfig.getParameterValue(MapperConfig.MAPPER_NAME_THRESHOLD_MIN));
        if (mapperHelper.existDimensionValues(natures)) {
            threshold = Double.valueOf(mapperConfig.getParameterValue(MapperConfig.MAPPER_VALUE_THRESHOLD));
            minThreshold = Double.valueOf(mapperConfig.getParameterValue(MapperConfig.MAPPER_VALUE_THRESHOLD_MIN));
        }

        return getThreshold(threshold, minThreshold, queryContext.getMapModeEnum());
    }

}
