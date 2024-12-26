package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ChatContext;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.ChatContextService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.chat.server.util.ResultFormatter;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Objects;

public class SqlExecutor implements ChatQueryExecutor {

    @SneakyThrows
    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        QueryResult queryResult = doExecute(executeContext);

        if (queryResult != null) {
            String textResult = ResultFormatter.transform2TextNew(queryResult.getQueryColumns(),
                    queryResult.getQueryResults());
            queryResult.setTextResult(textResult);

            if (queryResult.getQueryState().equals(QueryState.SUCCESS)
                    && queryResult.getQueryMode().equals(LLMSqlQuery.QUERY_MODE)) {
                Text2SQLExemplar exemplar =
                        JsonUtil.toObject(
                                JsonUtil.toString(executeContext.getParseInfo().getProperties()
                                        .get(Text2SQLExemplar.PROPERTY_KEY)),
                                Text2SQLExemplar.class);

                MemoryService memoryService = ContextUtils.getBean(MemoryService.class);
                memoryService.createMemory(ChatMemory.builder().queryId(queryResult.getQueryId())
                        .agentId(executeContext.getAgent().getId()).status(MemoryStatus.PENDING)
                        .question(exemplar.getQuestion()).sideInfo(exemplar.getSideInfo())
                        .dbSchema(exemplar.getDbSchema()).s2sql(exemplar.getSql())
                        .createdBy(executeContext.getRequest().getUser().getName())
                        .updatedBy(executeContext.getRequest().getUser().getName())
                        .createdAt(new Date()).build());
            }
        }

        return queryResult;
    }

    @SneakyThrows
    private QueryResult doExecute(ExecuteContext executeContext) {
        SemanticLayerService semanticLayer = ContextUtils.getBean(SemanticLayerService.class);
        ChatContextService chatContextService = ContextUtils.getBean(ChatContextService.class);

        ChatContext chatCtx =
                chatContextService.getOrCreateContext(executeContext.getRequest().getChatId());
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        if (Objects.isNull(parseInfo.getSqlInfo())
                || StringUtils.isBlank(parseInfo.getSqlInfo().getCorrectedS2SQL())) {
            return null;
        }

        QuerySqlReq sqlReq =
                QuerySqlReq.builder().sql(parseInfo.getSqlInfo().getCorrectedS2SQL()).build();
        sqlReq.setSqlInfo(parseInfo.getSqlInfo());
        sqlReq.setDataSetId(parseInfo.getDataSetId());

        long startTime = System.currentTimeMillis();
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryId(executeContext.getRequest().getQueryId());
        queryResult.setChatContext(parseInfo);
        queryResult.setQueryMode(parseInfo.getQueryMode());
        queryResult.setQueryTimeCost(System.currentTimeMillis() - startTime);
        SemanticQueryResp queryResp =
                semanticLayer.queryByReq(sqlReq, executeContext.getRequest().getUser());
        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
            queryResult.setQuerySql(queryResp.getSql());
            queryResult.setQueryResults(queryResp.getResultList());
            queryResult.setQueryColumns(queryResp.getColumns());
            queryResult.setQueryState(QueryState.SUCCESS);
            queryResult.setErrorMsg(queryResp.getErrorMsg());
            chatCtx.setParseInfo(parseInfo);
            chatContextService.updateContext(chatCtx);
        } else {
            queryResult.setQueryState(QueryState.INVALID);
        }

        return queryResult;
    }
}
