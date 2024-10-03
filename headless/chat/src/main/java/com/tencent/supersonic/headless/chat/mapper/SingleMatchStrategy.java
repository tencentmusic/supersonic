package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.MapResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public abstract class SingleMatchStrategy<T extends MapResult> extends BaseMatchStrategy<T> {
    @Autowired
    protected MapperConfig mapperConfig;
    @Autowired
    protected MapperHelper mapperHelper;

    public List<T> detect(ChatQueryContext chatQueryContext, List<S2Term> terms,
            Set<Long> detectDataSetIds) {
        Map<Integer, Integer> regOffsetToLength = mapperHelper.getRegOffsetToLength(terms);
        String text = chatQueryContext.getQueryText();
        Set<T> results = new HashSet<>();

        Set<String> detectSegments = new HashSet<>();

        for (Integer startIndex = 0; startIndex <= text.length() - 1;) {

            for (Integer index = startIndex; index <= text.length();) {
                int offset = mapperHelper.getStepOffset(terms, startIndex);
                index = mapperHelper.getStepIndex(regOffsetToLength, index);
                if (index <= text.length()) {
                    String detectSegment = text.substring(startIndex, index).trim();
                    detectSegments.add(detectSegment);
                    List<T> oneRoundResults =
                            detectByStep(chatQueryContext, detectDataSetIds, detectSegment, offset);
                    selectResultInOneRound(results, oneRoundResults);
                }
            }
            startIndex = mapperHelper.getStepIndex(regOffsetToLength, startIndex);
        }
        return new ArrayList<>(results);
    }

    public abstract List<T> detectByStep(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds, String detectSegment, int offset);
}
