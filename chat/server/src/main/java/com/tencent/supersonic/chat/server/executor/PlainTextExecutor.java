package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.S2ChatModelProvider;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;

import java.util.Collections;

public class PlainTextExecutor implements ChatExecutor {

    @Override
    public QueryResult execute(ChatExecuteContext chatExecuteContext) {
        if (!"PLAIN_TEXT".equals(chatExecuteContext.getParseInfo().getQueryMode())) {
            return null;
        }

        Prompt prompt = PromptTemplate.from(chatExecuteContext.getQueryText())
                .apply(Collections.EMPTY_MAP);

        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent chatAgent = agentService.getAgent(chatExecuteContext.getAgentId());

        ChatLanguageModel chatLanguageModel = S2ChatModelProvider.provide(chatAgent.getLlmConfig());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());

        QueryResult result = new QueryResult();
        result.setQueryState(QueryState.SUCCESS);
        result.setQueryMode(chatExecuteContext.getParseInfo().getQueryMode());
        result.setTextResult(response.content().text());

        return result;
    }
}
