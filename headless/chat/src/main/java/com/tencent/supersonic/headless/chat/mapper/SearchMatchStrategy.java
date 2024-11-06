package com.tencent.supersonic.headless.chat.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.chat.knowledge.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SearchMatchStrategy encapsulates a concrete matching algorithm executed during search process.
 */
@Service
public class SearchMatchStrategy extends BaseMatchStrategy<HanlpMapResult> {

    private static final int SEARCH_SIZE = 3;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private MapperHelper mapperHelper;

    @Override
    public Map<MatchText, List<HanlpMapResult>> match(ChatQueryContext chatQueryContext,
            List<S2Term> originals, Set<Long> detectDataSetIds) {
        String text = chatQueryContext.getRequest().getQueryText();
        Map<Integer, Integer> regOffsetToLength = mapperHelper.getRegOffsetToLength(originals);

        List<Integer> detectIndexList = Lists.newArrayList();

        for (Integer index = 0; index < text.length();) {

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
                List<HanlpMapResult> hanlpMapResults =
                        knowledgeBaseService.prefixSearch(detectSegment, SearchService.SEARCH_SIZE,
                                chatQueryContext.getModelIdToDataSetIds(), detectDataSetIds);
                List<HanlpMapResult> suffixHanlpMapResults =
                        knowledgeBaseService.suffixSearch(detectSegment, SEARCH_SIZE,
                                chatQueryContext.getModelIdToDataSetIds(), detectDataSetIds);
                hanlpMapResults.addAll(suffixHanlpMapResults);
                MatchText matchText =
                        MatchText.builder().regText(regText).detectSegment(detectSegment).build();
                regTextMap.put(matchText, hanlpMapResults);
            }
        });
        return regTextMap;
    }
}
