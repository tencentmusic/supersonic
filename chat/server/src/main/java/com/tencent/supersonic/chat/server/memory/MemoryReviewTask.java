package com.tencent.supersonic.chat.server.memory;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MemoryReviewTask {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    private static final String INSTRUCTION =
            ""
                    + "#Role: You are a senior data engineer experienced in writing SQL.\n"
                    + "#Task: Your will be provided with a user question and the SQL written by junior engineer,"
                    + "please take a review and give your opinion.\n"
                    + "#Rules: "
                    + "1.ALWAYS follow the output format: `opinion=(POSITIVE|NEGATIVE),comment=(your comment)`."
                    + "2.ALWAYS recognize `数据日期` as the date field."
                    + "3.IGNORE `数据日期` if not expressed in the `Question`."
                    + "#Question: %s\n"
                    + "#Schema: %s\n"
                    + "#SideInfo: %s\n"
                    + "#SQL: %s\n"
                    + "#Response: ";

    private static final Pattern OUTPUT_PATTERN = Pattern.compile("opinion=(.*),.*comment=(.*)");

    @Autowired private MemoryService memoryService;

    @Autowired private AgentService agentService;

    @Scheduled(fixedDelay = 60 * 1000)
    public void review() {
        try {
            memoryService.getMemoriesForLlmReview().stream().forEach(this::processMemory);
        } catch (Exception e) {
            log.error("Exception occurred during memory review task", e);
        }
    }

    private void processMemory(ChatMemoryDO m) {
        Agent chatAgent = agentService.getAgent(m.getAgentId());
        if (Objects.isNull(chatAgent) || !chatAgent.enableMemoryReview()) {
            log.debug("Agent id {} not found or memory review disabled", m.getAgentId());
            return;
        }
        String promptStr = createPromptString(m);
        Prompt prompt = PromptTemplate.from(promptStr).apply(Collections.EMPTY_MAP);

        keyPipelineLog.info("MemoryReviewTask reqPrompt:\n{}", promptStr);
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatAgent.getModelConfig());
        if (Objects.nonNull(chatLanguageModel)) {
            String response = chatLanguageModel.generate(prompt.toUserMessage()).content().text();
            keyPipelineLog.info("MemoryReviewTask modelResp:\n{}", response);
            processResponse(response, m);
        } else {
            log.debug("ChatLanguageModel not found for agent:{}", chatAgent.getId());
        }
    }

    private String createPromptString(ChatMemoryDO m) {
        return String.format(
                INSTRUCTION, m.getQuestion(), m.getDbSchema(), m.getSideInfo(), m.getS2sql());
    }

    private void processResponse(String response, ChatMemoryDO m) {
        Matcher matcher = OUTPUT_PATTERN.matcher(response);
        if (matcher.find()) {
            m.setLlmReviewRet(MemoryReviewResult.getMemoryReviewResult(matcher.group(1)));
            m.setLlmReviewCmt(matcher.group(2));
            // directly enable memory if the LLM determines it positive
            if (MemoryReviewResult.POSITIVE.equals(m.getLlmReviewRet())) {
                memoryService.enableMemory(m);
            }
            memoryService.updateMemory(m);
        }
    }
}
