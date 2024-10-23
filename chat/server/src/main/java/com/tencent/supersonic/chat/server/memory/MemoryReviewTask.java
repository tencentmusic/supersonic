package com.tencent.supersonic.chat.server.memory;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.server.utils.ModelConfigHelper;
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

    public static final String APP_KEY = "MEMORY_REVIEW";
    private static final String INSTRUCTION = ""
            + "#Role: You are a senior data engineer experienced in writing SQL."
            + "\n#Task: Your will be provided with a user question and the SQL written by a junior engineer,"
            + "please take a review and give your opinion." + "\n#Rules: "
            + "\n1.ALWAYS follow the output format: `opinion=(POSITIVE|NEGATIVE),comment=(your comment)`."
            + "\n2.NO NEED to check date filters as the junior engineer seldom makes mistakes in this regard."
            + "\n#Question: %s" + "\n#Schema: %s" + "\n#SideInfo: %s" + "\n#SQL: %s"
            + "\n#Response: ";

    private static final Pattern OUTPUT_PATTERN = Pattern.compile("opinion=(.*),.*comment=(.*)");

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AgentService agentService;

    public MemoryReviewTask() {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(INSTRUCTION).name("记忆启用评估").appModule(AppModule.CHAT)
                        .description("通过大模型对记忆做正确性评估以决定是否启用").enable(false).build());
    }

    @Scheduled(fixedDelay = 60 * 1000)
    public void review() {
        memoryService.getMemoriesForLlmReview().stream().forEach(memory -> {
            try {
                processMemory(memory);
            } catch (Exception e) {
                log.error("Exception occurred while processing memory with id {}: {}",
                        memory.getId(), e.getMessage(), e);
            }
        });
    }

    private void processMemory(ChatMemoryDO m) {
        Agent chatAgent = agentService.getAgent(m.getAgentId());
        if (Objects.isNull(chatAgent)) {
            log.warn("Agent id {} not found or memory review disabled", m.getAgentId());
            return;
        }

        ChatApp chatApp = chatAgent.getChatAppConfig().get(APP_KEY);
        if (Objects.isNull(chatApp) || !chatApp.isEnable()) {
            return;
        }

        String promptStr = createPromptString(m, chatApp.getPrompt());
        Prompt prompt = PromptTemplate.from(promptStr).apply(Collections.EMPTY_MAP);

        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(ModelConfigHelper.getChatModelConfig(chatApp));
        if (Objects.nonNull(chatLanguageModel)) {
            String response = chatLanguageModel.generate(prompt.toUserMessage()).content().text();
            keyPipelineLog.info("MemoryReviewTask modelReq:\n{} \nmodelResp:\n{}", promptStr,
                    response);
            processResponse(response, m);
        } else {
            log.debug("ChatLanguageModel not found for agent:{}", chatAgent.getId());
        }
    }

    private String createPromptString(ChatMemoryDO m, String promptTemplate) {
        return String.format(promptTemplate, m.getQuestion(), m.getDbSchema(), m.getSideInfo(),
                m.getS2sql());
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
