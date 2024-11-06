package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.MapResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public abstract class BatchMatchStrategy<T extends MapResult> extends BaseMatchStrategy<T> {

    @Autowired
    protected MapperConfig mapperConfig;

    @Override
    public List<T> detect(ChatQueryContext chatQueryContext, List<S2Term> terms,
            Set<Long> detectDataSetIds) {

        String text = chatQueryContext.getRequest().getQueryText();
        Set<String> detectSegments = new HashSet<>();

        int embeddingTextSize = Integer
                .valueOf(mapperConfig.getParameterValue(MapperConfig.EMBEDDING_MAPPER_TEXT_SIZE));

        int embeddingTextStep = Integer
                .valueOf(mapperConfig.getParameterValue(MapperConfig.EMBEDDING_MAPPER_TEXT_STEP));

        for (int startIndex = 0; startIndex < text.length(); startIndex += embeddingTextStep) {
            int endIndex = Math.min(startIndex + embeddingTextSize, text.length());
            String detectSegment = text.substring(startIndex, endIndex).trim();
            detectSegments.add(detectSegment);
        }
        return detectByBatch(chatQueryContext, detectDataSetIds, detectSegments);
    }

    public abstract List<T> detectByBatch(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds, Set<String> detectSegments);
}
