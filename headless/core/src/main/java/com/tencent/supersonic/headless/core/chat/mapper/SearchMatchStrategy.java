package com.tencent.supersonic.headless.core.chat.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.core.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.core.chat.knowledge.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SearchMatchStrategy encapsulates a concrete matching algorithm
 * executed during search process.
 */
@Service
public class SearchMatchStrategy extends BaseMatchStrategy<HanlpMapResult> {

    private static final int SEARCH_SIZE = 3;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public Map<MatchText, List<HanlpMapResult>> match(QueryContext queryContext, List<S2Term> originals,
            Set<Long> detectDataSetIds) {
        String text = queryContext.getQueryText();
        Map<Integer, Integer> regOffsetToLength = getRegOffsetToLength(originals);

        List<Integer> detectIndexList = Lists.newArrayList();

        for (Integer index = 0; index < text.length(); ) {

            if (index < text.length()) {
                detectIndexList.add(index);
            }
            Integer regLength = regOffsetToLength.get(index);
            if (Objects.nonNull(regLength)) {
                index = index + regLength;
            } else {
                index++;
            }
        }
        Map<MatchText, List<HanlpMapResult>> regTextMap = new ConcurrentHashMap<>();
        detectIndexList.stream().parallel().forEach(detectIndex -> {
                    String regText = text.substring(0, detectIndex);
                    String detectSegment = text.substring(detectIndex);

                    if (StringUtils.isNotEmpty(detectSegment)) {
                        List<HanlpMapResult> hanlpMapResults = knowledgeBaseService.prefixSearch(detectSegment,
                                SearchService.SEARCH_SIZE, queryContext.getModelIdToDataSetIds(), detectDataSetIds);
                        List<HanlpMapResult> suffixHanlpMapResults = knowledgeBaseService.suffixSearch(
                                detectSegment, SEARCH_SIZE, queryContext.getModelIdToDataSetIds(), detectDataSetIds);
                        hanlpMapResults.addAll(suffixHanlpMapResults);
                        // remove entity name where search
                        hanlpMapResults = hanlpMapResults.stream().filter(entry -> {
                            List<String> natures = entry.getNatures().stream()
                                    .filter(nature -> !nature.endsWith(DictWordType.ENTITY.getType()))
                                    .collect(Collectors.toList());
                            if (CollectionUtils.isEmpty(natures)) {
                                return false;
                            }
                            return true;
                        }).collect(Collectors.toList());
                        MatchText matchText = MatchText.builder()
                                .regText(regText)
                                .detectSegment(detectSegment)
                                .build();
                        regTextMap.put(matchText, hanlpMapResults);
                    }
                }
        );
        return regTextMap;
    }

    @Override
    public boolean needDelete(HanlpMapResult oneRoundResult, HanlpMapResult existResult) {
        return false;
    }

    @Override
    public String getMapKey(HanlpMapResult a) {
        return null;
    }

    @Override
    public void detectByStep(QueryContext queryContext, Set<HanlpMapResult> existResults, Set<Long> detectDataSetIds,
            String detectSegment, int offset) {

    }

}
