package com.tencent.supersonic.chat.server.memory;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.provider.ChatLanguageModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MemoryReviewTask {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    private static final String INSTRUCTION = ""
            + "#Role: You are a senior data engineer experienced in writing SQL.\n"
            + "#Task: Your will be provided with a user question and the SQL written by junior engineer,"
            + "please take a review and give your opinion.\n"
            + "#Rules: "
            + "1.ALWAYS follow the output format: `opinion=(POSITIVE|NEGATIVE),comment=(your comment)`."
            + "2.DO NOT check the usage of `数据日期` field and `datediff()` function.\n"
            + "#Question: %s\n"
            + "#Schema: %s\n"
            + "#SQL: %s\n"
            + "#Response: ";

    private static final Pattern OUTPUT_PATTERN = Pattern.compile("opinion=(.*),.*comment=(.*)");

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AgentService agentService;

    @Scheduled(fixedDelay = 60 * 1000)
    public void review() {
        memoryService.getMemoriesForLlmReview().stream()
                .forEach(m -> {
                    Agent chatAgent = agentService.getAgent(m.getAgentId());
                    String promptStr = String.format(INSTRUCTION, m.getQuestion(), m.getDbSchema(), m.getS2sql());
                    Prompt prompt = PromptTemplate.from(promptStr).apply(Collections.EMPTY_MAP);

                    keyPipelineLog.info("MemoryReviewTask reqPrompt:{}", promptStr);
                    ChatLanguageModel chatLanguageModel = ChatLanguageModelProvider.provide(chatAgent.getLlmConfig());
                    String response = chatLanguageModel.generate(prompt.toUserMessage()).content().text();
                    keyPipelineLog.info("MemoryReviewTask modelResp:{}", response);

                    Matcher matcher = OUTPUT_PATTERN.matcher(response);
                    if (matcher.find()) {
                        m.setLlmReviewRet(MemoryReviewResult.valueOf(matcher.group(1)));
                        m.setLlmReviewCmt(matcher.group(2));
                        memoryService.updateMemory(m);
                    }
                });
    }
}
