package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.common.util.JsonUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RewriteQueryGeneration {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");
    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private RewriteExamplarLoader rewriteExamplarLoader;

    @Autowired
    private SqlPromptGenerator sqlPromptGenerator;

    public String generation(String currentPromptStr) {
        //1.retriever sqlExamples

        List<Map<String, String>> rewriteExamples = rewriteExamplarLoader.getRewriteExamples().stream().map(o -> {
            return JsonUtil.toMap(JsonUtil.toString(o), String.class, String.class);
        }).collect(Collectors.toList());

        //2.generator linking and sql prompt by sqlExamples,and generate response.
        String promptStr = sqlPromptGenerator.generateRewritePrompt(rewriteExamples) + currentPromptStr;

        Prompt prompt = PromptTemplate.from(JsonUtil.toString(promptStr)).apply(new HashMap<>());
        keyPipelineLog.info("request prompt:{}", prompt.toSystemMessage());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toSystemMessage());
        String result = response.content().text();
        keyPipelineLog.info("model response:{}", result);
        //3.format response.
        String rewriteQuery = response.content().text();

        return rewriteQuery;
    }
}
