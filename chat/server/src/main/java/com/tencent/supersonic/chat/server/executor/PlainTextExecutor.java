package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlainTextExecutor implements ChatQueryExecutor {

    public static final String APP_KEY = "SMALL_TALK";
    private static final String INSTRUCTION = "#Role: You are a nice person to talk to."
            + "\n#Task: Respond quickly and nicely to the user."
            + "\n#Rules: 1.ALWAYS use the same language as the `#Current Input`."
            + "\n#History Inputs: %s" + "\n#Current Input: %s" + "\n#Response: ";

    public PlainTextExecutor() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("闲聊对话")
                .appModule(AppModule.CHAT).description("直接将原始输入透传大模型").enable(false).build());
    }

    @Override
    public boolean accept(ExecuteContext executeContext) {
        return "PLAIN_TEXT".equals(executeContext.getParseInfo().getQueryMode());
    }

    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent chatAgent = agentService.getAgent(executeContext.getAgent().getId());
        ChatApp chatApp = chatAgent.getChatAppConfig().get(APP_KEY);
        if (Objects.isNull(chatApp) || !chatApp.isEnable()) {
            return null;
        }

        String promptStr = String.format(chatApp.getPrompt(), getHistoryInputs(executeContext),
                executeContext.getRequest().getQueryText());
        Prompt prompt = PromptTemplate.from(promptStr).apply(Collections.EMPTY_MAP);
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatApp.getChatModelConfig());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());

        QueryResult result = new QueryResult();
        result.setQueryState(QueryState.SUCCESS);
        result.setQueryMode(executeContext.getParseInfo().getQueryMode());
        result.setTextResult(response.content().text());

        return result;
    }

    private String getHistoryInputs(ExecuteContext executeContext) {
        StringBuilder historyInput = new StringBuilder();
        List<QueryResp> queryResps = getHistoryQueries(executeContext.getRequest().getChatId(), 5);
        queryResps.forEach(p -> {
            historyInput.append(p.getQueryText());
            historyInput.append(";");

        });

        return historyInput.toString();
    }

    private List<QueryResp> getHistoryQueries(int chatId, int multiNum) {
        ChatManageService chatManageService = ContextUtils.getBean(ChatManageService.class);
        List<QueryResp> contextualParseInfoList = chatManageService.getChatQueries(chatId).stream()
                .filter(q -> Objects.nonNull(q.getQueryResult())
                        && q.getQueryResult().getQueryState() == QueryState.SUCCESS)
                .collect(Collectors.toList());

        List<QueryResp> contextualList = contextualParseInfoList.subList(0,
                Math.min(multiNum, contextualParseInfoList.size()));
        Collections.reverse(contextualList);

        return contextualList;
    }
}
