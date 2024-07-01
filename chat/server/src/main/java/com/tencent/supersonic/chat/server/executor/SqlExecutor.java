package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.chat.server.util.ResultFormatter;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.server.facade.service.ChatQueryService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import java.util.Date;

public class SqlExecutor implements ChatExecutor {

    @SneakyThrows
    @Override
    public QueryResult execute(ChatExecuteContext chatExecuteContext) {
        ExecuteQueryReq executeQueryReq = buildExecuteReq(chatExecuteContext);
        ChatQueryService chatQueryService = ContextUtils.getBean(ChatQueryService.class);
        QueryResult queryResult = chatQueryService.performExecution(executeQueryReq);
        if (queryResult != null) {
            String textResult = ResultFormatter.transform2TextNew(queryResult.getQueryColumns(),
                    queryResult.getQueryResults());
            queryResult.setTextResult(textResult);

            if (queryResult.getQueryState().equals(QueryState.SUCCESS)
                    && queryResult.getQueryMode().equals(LLMSqlQuery.QUERY_MODE)) {
                MemoryService memoryService = ContextUtils.getBean(MemoryService.class);
                memoryService.createMemory(ChatMemoryDO.builder()
                        .agentId(chatExecuteContext.getAgentId())
                        .status(MemoryStatus.PENDING)
                        .question(chatExecuteContext.getQueryText())
                        .s2sql(chatExecuteContext.getParseInfo().getSqlInfo().getS2SQL())
                        .dbSchema(buildSchemaStr(chatExecuteContext.getParseInfo()))
                        .createdBy(chatExecuteContext.getUser().getName())
                        .createdAt(new Date())
                        .build());
            }
        }

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

    public String buildSchemaStr(SemanticParseInfo parseInfo) {
        String tableStr = parseInfo.getDataSet().getName();
        StringBuilder metricStr = new StringBuilder();
        StringBuilder dimensionStr = new StringBuilder();

        parseInfo.getMetrics().stream().forEach(
                metric -> {
                    metricStr.append(metric.getName());
                    if (StringUtils.isNotEmpty(metric.getDescription())) {
                        metricStr.append(" COMMENT '" + metric.getDescription() + "'");
                    }
                    if (StringUtils.isNotEmpty(metric.getDefaultAgg())) {
                        metricStr.append(" AGGREGATE '" + metric.getDefaultAgg().toUpperCase() + "'");
                    }
                    metricStr.append(",");
                }
        );

        parseInfo.getDimensions().stream().forEach(
                dimension -> {
                    dimensionStr.append(dimension.getName());
                    if (StringUtils.isNotEmpty(dimension.getDescription())) {
                        dimensionStr.append(" COMMENT '" + dimension.getDescription() + "'");
                    }
                    dimensionStr.append(",");
                }
        );

        String template = "Table: %s, Metrics: [%s], Dimensions: [%s]";
        return String.format(template, tableStr, metricStr, dimensionStr);
    }

}
