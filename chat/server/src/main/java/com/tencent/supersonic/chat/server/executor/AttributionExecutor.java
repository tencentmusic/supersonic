package com.tencent.supersonic.chat.server.executor;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import com.tencent.supersonic.headless.server.service.ModelService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import jdk.jfr.Description;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class AttributionExecutor implements ChatQueryExecutor {
    public static final String APP_KEY = "S2SQL_PARSER";

    @Override
    public boolean accept(ExecuteContext executeContext) {
//        return "Attribution_Analysis".equals(executeContext.getParseInfo().getQueryMode());
        return false;
    }

    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        QueryResult queryResult = new QueryResult();

        // 判断是否多步骤分析
        if (executeContext.getParseInfo().getSqlList() != null
                && !executeContext.getParseInfo().getSqlList().isEmpty()) {

            // 多步骤执行模式
            return executeMultiStepAnalysis(executeContext, queryResult);
        } else {
            // 原有单SQL执行模式
            return executeSingleStepAnalysis(executeContext, queryResult);
        }
    }

    @Override
    public TokenStream streamExecute(ExecuteContext executeContext) {
        return null;
    }

    // 原有单SQL执行方法（保持兼容）
    private QueryResult executeSingleStepAnalysis(ExecuteContext executeContext,
            QueryResult queryResult) {
        String sql = executeContext.getParseInfo().getSqlInfo().getCorrectedS2SQL();
        String sqlResult = executeSqlAndRecord(executeContext, sql, queryResult);
        String explanation = generateExplanation(sqlResult, executeContext);
        queryResult.setTextResult(explanation);
        return queryResult;
    }

    private static String executeSqlAndRecord(ExecuteContext executeContext, String querySql,
            QueryResult queryResult) {
        try {
            // 构造查询环境
            SqlUtils sqlUtils = ContextUtils.getBean(SqlUtils.class);
            QueryStatement queryStatement = new QueryStatement();
            ModelService modelService = ContextUtils.getBean(ModelService.class);
            DatabaseResp database = modelService.getDatabaseByModelId(19L);
            Ontology ontology = new Ontology();
            ontology.setDatabase(database);
            queryStatement.setOntology(ontology);
            SemanticQueryResp semanticQueryResp = new SemanticQueryResp();
            // 初始化 SQL 工具并执行 SQL 查询
            SqlUtils sqlUtil = sqlUtils.init(database);

            sqlUtil.queryInternal(querySql, semanticQueryResp);
            semanticQueryResp.setSql(querySql);

            // 若查询出错，则抛出异常
            if (StringUtils.isNotEmpty(semanticQueryResp.getErrorMsg())) {
                throw new RuntimeException("SQL执行失败: " + semanticQueryResp.getErrorMsg());
            }

            // 记录查询结果
            queryResult.setQuerySql(querySql);
            queryResult.setQueryColumns(semanticQueryResp.getColumns());
            queryResult.setQueryResults(semanticQueryResp.getResultList());
            queryResult.setQueryMode(executeContext.getParseInfo().getQueryMode());
            if (CollectionUtils.isEmpty(semanticQueryResp.getResultList())) {
                log.warn("SQL执行结果为空");
                return "[]";
            }

            String resultJson = JSON.toJSONString(semanticQueryResp.getResultList());
            log.info("SQL执行成功, 结果行数: {}", semanticQueryResp.getResultList().size());
            return resultJson;
        } catch (Exception e) {
            log.error("执行SQL查询时发生异常", e);
            throw new RuntimeException("SQL执行失败: " + e.getMessage(), e);
        }
    }

    // 数据结构定义
    @Data
    public static class StepExecutionResult {
        private int stepNumber;
        private String sql;
        private List<String> columns;
        private List<Map<String, Object>> data;
        private String analysis;
        private String error;
    }

    private QueryResult executeMultiStepAnalysis(ExecuteContext executeContext,
            QueryResult finalResult) {
        List<String> sqlList = executeContext.getParseInfo().getSqlList();
        List<StepExecutionResult> stepResults = new ArrayList<>();
        StringBuilder combinedAnalysis = new StringBuilder();

        // 1. 分步执行所有SQL
        for (int i = 0; i < sqlList.size(); i++) {
            String sql = sqlList.get(i);
            StepExecutionResult stepResult = new StepExecutionResult();
            QueryResult queryResult = new QueryResult();
            stepResult.setStepNumber(i + 1);

            try {
                // 执行单个SQL
                String sqlResult = executeSqlAndRecord(executeContext, sql, queryResult);

                // 生成步骤分析报告
                String stepAnalysis =
                        generateStepExplanation(executeContext, sqlResult, i + 1, sqlList.size());

                stepResult.setAnalysis(stepAnalysis);
                stepResults.add(stepResult);

                // 合并到总分析
                combinedAnalysis.append("## 步骤").append(i + 1).append("分析:\n").append(stepAnalysis)
                        .append("\n\n");

            } catch (Exception e) {
                stepResult.setError(e.getMessage());
                stepResults.add(stepResult);
                log.error("步骤{}执行失败", i + 1, e);
            }
        }

        // 2. 生成综合分析报告
        if (!stepResults.isEmpty()) {
            String fullAnalysis = generateFinalAnalysis(executeContext, sqlList.size(),
                    combinedAnalysis.toString());

            // // 3. 构建最终结果
            // MultiStepQueryResult result = new MultiStepQueryResult();
            // result.setStepResults(stepResults);
            // result.setFinalAnalysis(fullAnalysis);
            // result.setQueryMode(executeContext.getParseInfo().getQueryMode());
            //
            // finalResult.setObjectResult(result);
            finalResult.setTextResult(fullAnalysis);
        }

        finalResult.setQueryMode(executeContext.getParseInfo().getQueryMode());
        return finalResult;
    }

    private String generateStepExplanation(ExecuteContext executeContext, String resultJson,
            int step, int totalSteps) {
        String prompt = String.format("""
                #步骤分析任务 (%d/%d)
                原始问题：%s
                当前步骤数据：%s
                请分析：
                1. 本步骤数据的关键发现
                2. 与问题的关联性
                3. 用简洁的3-5句话总结
                """, step, totalSteps, executeContext.getRequest().getQueryText(), resultJson);

        AttributionAnalyzer analyzer = createAnalyzer(executeContext);
        return analyzer.analyze(prompt);
    }

    private String generateFinalAnalysis(ExecuteContext executeContext, int totalSteps,
            String stepAnalyses) {
        String prompt = String.format("""
                #综合分析报告生成
                原始问题：%s
                已完成分析步骤：%d个
                各步骤摘要：
                %s
                请生成：
                1. 关键结论总结
                2. 主要影响因素排序
                3. 可行动建议
                格式要求：
                ## 关键结论
                ## 因素分析
                ## 建议措施
                """, executeContext.getRequest().getQueryText(), totalSteps, stepAnalyses);

        AttributionAnalyzer analyzer = createAnalyzer(executeContext);
        return analyzer.analyze(prompt);
    }

    private AttributionAnalyzer createAnalyzer(ExecuteContext executeContext) {
        ChatApp chatApp = executeContext.getAgent().getChatAppConfig().get(APP_KEY);
        ChatLanguageModel model = getChatLanguageModel(chatApp.getChatModelConfig());
        return AiServices.create(AttributionAnalyzer.class, model);
    }

    public interface AttributionAnalyzer {
        @SystemMessage("你是一个专业的数据分析师,擅长归因分析。请对数据进行深入分析,找出关键影响因素。")
        @UserMessage("{{prompt}}")
        @Description("生成归因分析结果")
        String analyze(String prompt);
    }

    public String generateExplanation(String result, ExecuteContext executeContext) {
        try {
            // 1. 检查结果是否为空
            if (StringUtils.isBlank(result)) {
                log.warn("查询结果为空,无法生成归因分析");
                return "暂无数据可供分析";
            }

            // 2. 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请对以下数据进行归因分析:\n\n");
            prompt.append("用户的问题是:").append(executeContext.getRequest().getQueryText())
                    .append("\n");
            prompt.append("查询的数据结果是:").append(result).append("\n\n");
            prompt.append("要求:\n");
            prompt.append("1. 分析数据中的关键指标变化\n");
            prompt.append("2. 找出主要影响因素\n");
            prompt.append("3. 给出合理的解释\n");
            prompt.append("4. 使用简洁清晰的语言描述\n");

            // 3. 调用大模型生成分析结果
            AttributionAnalyzer analyzer = createAnalyzer(executeContext);
            String analysis = analyzer.analyze(prompt.toString());
            // 4. 记录日志
            log.info("归因分析生成成功");
            return analysis;
        } catch (Exception e) {
            log.error("生成归因分析异常", e);
            return "归因分析生成失败:" + e.getMessage();
        }
    }

    protected ChatLanguageModel getChatLanguageModel(ChatModelConfig modelConfig) {
        return ModelProvider.getChatModel(modelConfig);
    }
}
