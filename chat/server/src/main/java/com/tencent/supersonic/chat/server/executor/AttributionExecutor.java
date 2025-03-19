package com.tencent.supersonic.chat.server.executor;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.parser.AttributionParser;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import com.tencent.supersonic.headless.server.service.ModelService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import jdk.jfr.Description;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AttributionExecutor  implements ChatQueryExecutor{
    public static final String APP_KEY = "S2SQL_PARSER";
    @Override
    public boolean accept(ExecuteContext executeContext) {
        return "Attribution_Analysis".equals(executeContext.getParseInfo().getQueryMode());
    }

    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        QueryResult queryResult = new QueryResult();
        // 4. 执行SQL查询
        String sqlResult = executeSqlAndRecord(executeContext, queryResult);
        //5. 将查询结果发给大模型生成归因分析
        String explanation = generateExplanation(sqlResult, executeContext);
        // 6. 返回分析结果
        queryResult.setTextResult(explanation);
        return queryResult;
    }
    private static String executeSqlAndRecord(ExecuteContext executeContext, QueryResult queryResult) {
        try {
            // 构造查询环境
            SqlUtils sqlUtils = ContextUtils.getBean(SqlUtils.class);
            QueryStatement queryStatement = new QueryStatement();
            ModelService modelService = ContextUtils.getBean(ModelService.class);
            DatabaseResp database =
                    modelService.getDatabaseByModelId(19L);
            Ontology ontology = new Ontology();
            ontology.setDatabase(database);
            queryStatement.setOntology(ontology);
            SemanticQueryResp semanticQueryResp = new SemanticQueryResp();
            // 初始化 SQL 工具并执行 SQL 查询
            SqlUtils sqlUtil = sqlUtils.init(database);
            String querySql = executeContext.getParseInfo().getSqlInfo().getCorrectedS2SQL();
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
            prompt.append("用户的问题是:").append(executeContext.getRequest().getQueryText()).append("\n");
            prompt.append("查询的数据结果是:").append(result).append("\n\n");
            prompt.append("要求:\n");
            prompt.append("1. 分析数据中的关键指标变化\n");
            prompt.append("2. 找出主要影响因素\n");
            prompt.append("3. 给出合理的解释\n");
            prompt.append("4. 使用简洁清晰的语言描述\n");

            // 3. 调用大模型生成分析结果
            ChatApp chatApp = executeContext.getAgent().getChatAppConfig().get(APP_KEY);
            ChatLanguageModel model = getChatLanguageModel(chatApp.getChatModelConfig());
            AttributionAnalyzer analyzer = AiServices.create(AttributionAnalyzer.class, model);
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
