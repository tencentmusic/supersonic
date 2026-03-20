package com.tencent.supersonic.chat.server.executor;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleParams;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.service.ReportScheduleService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;

import java.util.Collections;
import java.util.Objects;

@Slf4j
public class ReportScheduleExecutor implements ChatQueryExecutor {

    public static final String APP_KEY = "SCHEDULE_PARAM_EXTRACT";
    public static final String QUERY_MODE = "REPORT_SCHEDULE";

    private static final String INSTRUCTION =
            "You are a report scheduling parameter extraction assistant. "
                    + "Extract the following fields from the user's natural language request "
                    + "and return them as JSON:\n\n" + "Field descriptions:\n"
                    + "- name: A short name for the report task\n"
                    + "- cronExpression: Spring Cron expression (6 fields: second minute hour day month weekday)\n"
                    + "- outputFormat: Output format, one of EXCEL / CSV / JSON (default EXCEL)\n\n"
                    + "Cron conversion rules (6 fields: second minute hour day month weekday):\n"
                    + "- Every day at 9am -> 0 0 9 * * ?\n" + "- Every day at 5pm -> 0 0 17 * * ?\n"
                    + "- Every Monday at 10am -> 0 0 10 ? * MON\n"
                    + "- 1st of every month at 2am -> 0 0 2 1 * ?\n"
                    + "- Weekdays at 8am -> 0 0 8 ? * MON-FRI\n\n"
                    + "Return JSON only, no other text.\n\n" + "User request: %s";

    public ReportScheduleExecutor() {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(INSTRUCTION).name("定时报表参数提取").appModule(AppModule.CHAT)
                        .description("通过大模型从自然语言中提取定时报表的调度参数（名称、Cron表达式、输出格式等）").enable(true)
                        .build());
    }

    @Override
    public boolean accept(ExecuteContext executeContext) {
        return QUERY_MODE.equals(executeContext.getParseInfo().getQueryMode());
    }

    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent chatAgent = agentService.getAgent(executeContext.getAgent().getId());
        ChatApp chatApp = chatAgent.getChatAppConfig().get(APP_KEY);
        if (Objects.isNull(chatApp) || !chatApp.isEnable()) {
            return null;
        }

        String queryText = executeContext.getRequest().getQueryText();
        User user = executeContext.getRequest().getUser();

        try {
            ScheduleParams params = extractParams(chatApp, queryText);
            validateCron(params.getCronExpression());

            SemanticParseInfo parseInfo = executeContext.getParseInfo();
            String queryConfig = buildQueryConfig(parseInfo);
            if (queryConfig == null) {
                QueryResult result = new QueryResult();
                result.setQueryMode(QUERY_MODE);
                result.setQueryState(QueryState.INVALID);
                result.setTextResult("当前会话的查询结果不支持定时订阅。请使用结构化查询（指定指标和维度）后再创建定时报表。");
                return result;
            }

            Long datasetId = parseInfo.getDataSetId();
            ReportScheduleService service = ContextUtils.getBean(ReportScheduleService.class);

            ReportScheduleDO schedule = new ReportScheduleDO();
            schedule.setName(params.getName());
            schedule.setDatasetId(datasetId);
            schedule.setCronExpression(params.getCronExpression());
            schedule.setOutputFormat(params.getOutputFormat());
            schedule.setQueryConfig(queryConfig);
            schedule.setEnabled(true);
            schedule.setOwnerId(user.getId());
            schedule.setTenantId(user.getTenantId());
            schedule.setCreatedBy(user.getName());

            ReportScheduleDO created = service.createSchedule(schedule);

            QueryResult result = new QueryResult();
            result.setQueryMode(QUERY_MODE);
            result.setQueryState(QueryState.SUCCESS);
            result.setTextResult(String.format("定时报表已创建\n- 名称：%s\n- Cron：%s\n- 格式：%s\n- 任务 ID：#%d",
                    created.getName(), created.getCronExpression(), created.getOutputFormat(),
                    created.getId()));
            return result;
        } catch (Exception e) {
            log.error("Failed to create report schedule from query: {}", queryText, e);
            QueryResult result = new QueryResult();
            result.setQueryMode(QUERY_MODE);
            result.setQueryState(QueryState.INVALID);
            result.setTextResult("Failed to create report schedule: " + e.getMessage());
            return result;
        }
    }

    private ScheduleParams extractParams(ChatApp chatApp, String queryText) {
        String promptStr = String.format(chatApp.getPrompt(), queryText);
        Prompt prompt = PromptTemplate.from(promptStr).apply(Collections.emptyMap());
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatApp.getChatModelConfig());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
        String responseText = response.content().text();
        return JSONObject.parseObject(responseText, ScheduleParams.class);
    }

    private String buildQueryConfig(SemanticParseInfo parseInfo) {
        if (parseInfo == null || parseInfo.getDataSetId() == null) {
            return null;
        }
        if (parseInfo.getMetrics() == null || parseInfo.getMetrics().isEmpty()) {
            return null;
        }
        if (QueryType.AGGREGATE.equals(parseInfo.getQueryType())
                || !parseInfo.getMetrics().isEmpty()) {
            QueryStructReq structReq = QueryReqBuilder.buildStructReq(parseInfo);
            structReq.setDataSetId(parseInfo.getDataSetId());
            return JsonUtil.toString(structReq);
        }
        return null;
    }

    private void validateCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("Cron expression cannot be empty");
        }
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
        }
    }
}
