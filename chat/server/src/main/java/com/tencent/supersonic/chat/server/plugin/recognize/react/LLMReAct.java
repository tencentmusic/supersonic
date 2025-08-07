package com.tencent.supersonic.chat.server.plugin.recognize.react;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.server.modeller.LLMSemanticModeller;
import com.tencent.supersonic.headless.server.utils.ModelConfigHelper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.service.AiServices;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class LLMReAct {
    public static final String APP_KEY = "LLM_REACT";
    public static final String INSTRUCTION = """
            你是一个问答专家，你必须始终独立做出决策，无需寻求用户的帮助，发挥你作为LLM的优势，追求简答的策略。
            限制条件说明:
            1. 你只需选择一个满足要求的工具
            2. 你只能主动行动，在计划行动时需要考虑到这一点
            3. 返回的参数名(arguments)必须和给出的名称完全一样
            #问题: {{question}}
            """;

    LLMReAct() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("MCP 提示词")
                .appModule(AppModule.CHAT).description("通过MCP协议 选择工具").enable(true).build());
    }

    private ChatLanguageModel getChatModel(Integer chatModelId) {
        ChatModelService chatModelService = ContextUtils.getBean(ChatModelService.class);
        ChatModel chatModel = chatModelService.getChatModel(chatModelId);
        ChatModelConfig chatModelConfig = chatModel.getConfig();
        return ModelProvider.getChatModel(chatModelConfig);
    }

    public LLMResp generateTool(Integer chatModelId, List<ToolSpecification> tool,
            String question) {
        ChatLanguageModel chatLanguageModel = getChatModel(chatModelId);
        Prompt prompt = generatePrompt(question, INSTRUCTION);

        Response<AiMessage> response =
                chatLanguageModel.generate(Collections.singletonList(prompt.toUserMessage()), tool);
        AiMessage content = response.content();
        if (content.hasToolExecutionRequests()) {
            if (StringUtils.isNotBlank(content.text())) {
                return null; // TODO
            }
            ToolExecutionRequest toolExecutionRequest = content.toolExecutionRequests().get(0);
            LLMResp llmResp = new LLMResp();
            llmResp.setQuery(question);
            llmResp.setSqlOutput(toolExecutionRequest.name());
            llmResp.setSchema(toolExecutionRequest.arguments());
            return llmResp;
        }
        return null;
    }

    private Prompt generatePrompt(String question, String prompt) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("question", question);
        return PromptTemplate.from(prompt).apply(variable);
    }

}
