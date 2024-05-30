package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.chat.server.util.ResultFormatter;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import lombok.SneakyThrows;

public class SqlExecutor implements ChatExecutor {

    @SneakyThrows
    @Override
    public QueryResult execute(ChatExecuteContext chatExecuteContext) {
        SemanticParseInfo parseInfo = chatExecuteContext.getParseInfo();
        if (PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            return null;
        }
        ExecuteQueryReq executeQueryReq = buildExecuteReq(chatExecuteContext);
        ChatQueryService chatQueryService = ContextUtils.getBean(ChatQueryService.class);
        QueryResult queryResult = chatQueryService.performExecution(executeQueryReq);
        String textResult = ResultFormatter.transform2TextNew(queryResult.getQueryColumns(),
                queryResult.getQueryResults());
        queryResult.setTextResult(textResult);
        return queryResult;
    }

    private ExecuteQueryReq buildExecuteReq(ChatExecuteContext chatExecuteContext) {
        SemanticParseInfo parseInfo = chatExecuteContext.getParseInfo();
        return ExecuteQueryReq.builder()
                .queryId(chatExecuteContext.getQueryId())
                .chatId(chatExecuteContext.getChatId())
                .queryText(chatExecuteContext.getQueryText())
                .parseInfo(parseInfo)
                .saveAnswer(chatExecuteContext.isSaveAnswer())
                .user(chatExecuteContext.getUser())
                .build();
    }

}
