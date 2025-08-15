package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.MapResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
        String text = chatQueryContext.getRequest().getQueryText();
        List<Supplier<List<T>>> tasks = new ArrayList<>();

        for (int startIndex = 0; startIndex <= text.length() - 1;) {
            for (int index = startIndex; index <= text.length();) {
                int offset = mapperHelper.getStepOffset(terms, startIndex);
                index = mapperHelper.getStepIndex(regOffsetToLength, index);
                if (index <= text.length()) {
                    String detectSegment = text.substring(startIndex, index).trim();
                    Supplier<List<T>> task =
                            createTask(chatQueryContext, detectDataSetIds, detectSegment, offset);
                    tasks.add(task);
                }
            }
            startIndex = mapperHelper.getStepIndex(regOffsetToLength, startIndex);
        }
        Set<T> results = executeTasks(tasks);
        return new ArrayList<>(results);
    }

    private Supplier<List<T>> createTask(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds, String detectSegment, int offset) {
        return () -> detectByStep(chatQueryContext, detectDataSetIds, detectSegment, offset);
    }

    public abstract List<T> detectByStep(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds, String detectSegment, int offset);
}
