package com.tencent.supersonic.chat.server.processor.execute;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.DimValuesConstants;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DataInterpretProcessor interprets query result to make it more readable to the users.
 */
public class DataInterpretProcessor implements ExecuteResultProcessor {
    private final QueryCache queryCache = ComponentFactory.getQueryCache();
    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    public static final String APP_KEY = "DATA_INTERPRETER";
    private static final String INSTRUCTION = ""
            + "#Role: You are a data expert who communicates with business users everyday."
            + "\n#Task: Your will be provided with a question asked by a user and the relevant "
            + "result data queried from the databases, please interpret the data and organize a brief answer."
            + "\n#Rules: " + "\n1.ALWAYS respond in the use the same language as the `#Question`."
            + "\n2.ALWAYS reference some key data in the `#Answer`."
            + "\n#Question:{{question}} #Data:{{data}} #Answer:";

    public DataInterpretProcessor() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("结果数据解读")
                .appModule(AppModule.CHAT).description("通过大模型对结果数据做提炼总结").enable(false).build());
    }


    @Override
    public void process(ExecuteContext executeContext, QueryResult queryResult) {
        String queryId = String.valueOf(executeContext.getRequest().getQueryId());
        String fullQueryKey = queryId + DimValuesConstants.FULL_QUERY;
        Boolean condition = (Boolean) queryCache.get(fullQueryKey);
        if (queryId != null && condition != null && condition){
            String text = getTipString(queryResult);
            queryResult.setTextSummary(text);
        }
        Agent agent = executeContext.getAgent();
        ChatApp chatApp = agent.getChatAppConfig().get(APP_KEY);
        if (Objects.isNull(chatApp) || !chatApp.isEnable()) {
            return;
        }

        Map<String, Object> variable = new HashMap<>();
        variable.put("question", executeContext.getRequest().getQueryText());
        variable.put("data", queryResult.getTextResult());

        Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variable);
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatApp.getChatModelConfig());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
        String anwser = response.content().text();
        keyPipelineLog.info("DataInterpretProcessor modelReq:\n{} \nmodelResp:\n{}", prompt.text(),
                anwser);
        if (StringUtils.isNotBlank(anwser)) {
            queryResult.setTextSummary(anwser);
        }
    }

    private String getTipString(QueryResult queryResult) {
        // 获取 SQL 语句
        String sql = queryResult.getQuerySql();

        // 定义字段与对应分类名称的映射
        Map<String, String> fieldToCategoryMap = new HashMap<>(4);
        fieldToCategoryMap.put("pid1_2022", "活跃场景一级分类");
        fieldToCategoryMap.put("pid2_2022", "活跃场景二级分类");
        fieldToCategoryMap.put("pid3_2022", "活跃场景三级分类");
        fieldToCategoryMap.put("pid4_2022", "活跃场景四级分类");

        // 从 SQL 中解析 WHERE 条件
        Map<String, String> whereConditions = parseWhereConditions(sql);

        // 提取 WHERE 条件中 pid1_2022, pid2_2022, pid3_2022, pid4_2022 的值
        List<String> matchedValues = whereConditions.entrySet().stream()
                .filter(entry -> fieldToCategoryMap.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        // 如果匹配到的值不为空，替换 "各级分类" 文本
        String matchedText = matchedValues.isEmpty() ? "各级分类" : String.join("，", matchedValues);

        // 获取 queryResults
        List<Map<String, Object>> queryResults = queryResult.getQueryResults();

        // 构建详细分类的文本内容
        StringBuilder detailedText = new StringBuilder();

        // 提取前三条数据生成文本
        for (int i = 0; i < Math.min(3, queryResults.size()); i++) {
            Map<String, Object> row = queryResults.get(i);
            detailedText.append(String.format("%s是%s，%s是%s，%s是%s，%s是%s\n",
                    fieldToCategoryMap.get("pid1_2022"), row.getOrDefault("pid1_2022", "全部"),
                    fieldToCategoryMap.get("pid2_2022"), row.getOrDefault("pid2_2022", "全部"),
                    fieldToCategoryMap.get("pid3_2022"), row.getOrDefault("pid3_2022", "全部"),
                    fieldToCategoryMap.get("pid4_2022"), row.getOrDefault("pid4_2022", "全部")
            ));
        }

        return String.format("\n提示：涉及分级的问题\n" +
                "%s 对应多种分类数据，如下所示，请选择如下细致分类，添加到问题中：\n" +
                "%s", matchedText, detailedText.toString());
    }
    private Map<String, String> parseWhereConditions(String sql) {
        Map<String, String> conditions = new HashMap<>();
        String whereClauseRegex = "WHERE(.*)";
        Matcher matcher = Pattern.compile(whereClauseRegex, Pattern.CASE_INSENSITIVE).matcher(sql);

        if (matcher.find()) {
            String whereClause = matcher.group(1);
            String[] conditionsArray = whereClause.split(" AND ");

            for (String condition : conditionsArray) {
                String[] keyValue = condition.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("`", "");
                    String value = keyValue[1].trim().replaceAll("'", "");
                    conditions.put(key, value);
                }
            }
        }

        return conditions;
    }
}
