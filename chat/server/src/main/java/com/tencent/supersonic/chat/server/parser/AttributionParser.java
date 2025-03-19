package com.tencent.supersonic.chat.server.parser;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.tencent.supersonic.chat.api.pojo.enums.IntentType;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class AttributionParser implements ChatQueryParser{

    private static final Set<String> ATTRIBUTION_KEYWORDS =
            Set.of("为什么", "原因", "因素", "归因", "下降原因", "增长来源");
    public static final String APP_KEY = "S2SQL_PARSER";
    public static final String INSTRUCTION =
            "#Role: You are a data analyst specialized in attribution analysis."
                    + "\n#Task: Generate SQL queries for attribution analysis based on user requirements."
                    + "\n#Rules:"
                    + "\n1. Compare metrics between baseline and comparison periods"
                    + "\n2. Calculate contribution of each dimension"
                    + "\n3. Use appropriate statistical methods for attribution"
                    + "\n4. Handle NULL values and data quality issues"
                    + "\n5. Generate clear and maintainable SQL"
                    + "\n6. 不要使用with和over语法，mysql版本不支持";
    @Override
    public boolean accept(ParseContext parseContext) {
        //1.意图识别是否是归因分析
        IntentType intentType = parseUserIntent(parseContext.getRequest().getQueryText());
        if (intentType == IntentType.ATTRIBUTION) {
            return true;
        }
        return false;
    }

    @Override
    public void parse(ParseContext parseContext) {
//2. 准备相关配置
        LLMReq llmReq = new LLMReq();

        llmReq.setQueryText(parseContext.getRequest().getQueryText());
        llmReq.setChatAppConfig(parseContext.getAgent().getChatAppConfig());
        // 3. 调用大模型生成SQL
        LLMResp llmResp = generate(llmReq);

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode("Attribution_Analysis");
        parseInfo.setId(1);
        parseInfo.getSqlInfo().setCorrectedS2SQL(llmResp.getSqlOutput());
        parseInfo.getSqlInfo().setResultType("text");
        parseContext.getResponse().getSelectedParses().add(parseInfo);
        parseContext.getResponse().setState(ParseResp.ParseState.COMPLETED);
    }

    private IntentType parseUserIntent(String question) {
        // TODO: 使用LLM解析用户查询意图
        // 使用大模型进行意图分类
//        String prompt = "判断问题类型：\n问题：" + question + "\n选项：数据查询、归因分析";
//        String response = LLMClient.query(prompt);
//        return response.contains("归因分析") ?
//               IntentType.ATTRIBUTION : IntentType.NORMAL_QUERY;
        // 如果包含归因关键词则认为是归因分析，否则返回 NORMAL_QUERY
        if (ATTRIBUTION_KEYWORDS.stream().anyMatch(question::contains)) {
            return IntentType.ATTRIBUTION;
        }
        return IntentType.NORMAL_QUERY;
    }

    public LLMResp generate(LLMReq llmReq) {
        LLMResp llmResp = new LLMResp();
        try {
            String attributionSql = generateAttributionSql(llmReq);
            llmResp.setSqlOutput(attributionSql);
        } catch (Exception e) {
            log.error("归因分析SQL生成失败", e);
            throw new RuntimeException("归因分析SQL生成失败", e);
        }
        return llmResp;
    }
    private String generateAttributionSql(LLMReq llmReq) {
        // 1. 获取基础信息
        ChatApp chatApp = llmReq.getChatAppConfig().get(APP_KEY);
        // 2. 构建归因分析提示语
        String prompt = generatePrompt(llmReq, chatApp);

        // 3. 调用LLM生成SQL
        ChatLanguageModel model = getChatLanguageModel(chatApp.getChatModelConfig());
        SemanticSqlExtractor extractor = AiServices.create(SemanticSqlExtractor.class, model);

        // 4. 解析LLM返回结果
        SemanticSql semanticSql = extractor.generateSemanticSql(prompt);
        String attributionSql = semanticSql.getSql();

        if (StringUtils.isBlank(attributionSql)) {
            log.error("归因分析SQL生成为空");
            throw new RuntimeException("归因分析SQL生成为空");
        }
        return attributionSql;

    }
    private String generatePrompt(LLMReq llmReq, ChatApp chatApp) {
        StringBuilder context = new StringBuilder();
        context.append(INSTRUCTION).append("\n\n");
        if (chatApp != null) {
            String fullPrompt = chatApp.getPrompt();
            // 查找“示例：”在字符串中的位置
            int exampleIndex = fullPrompt.indexOf("示例：");

            // 如果找到“示例：”，则截取该位置之前的内容
            if (exampleIndex != -1) {
                context.append(fullPrompt.substring(0, exampleIndex).trim()).append("\n\n");
            }else {
                // 如果没有找到“示例：”，则返回完整内容
                context.append(fullPrompt).append("\n\n");
            }
        }
        // 添加归因分析相关信息
        context.append("请根据以下信息生成归因分析SQL:\n")
                .append("问题:").append(llmReq.getQueryText()).append("\n")
                .append("要求:\n")
                .append("1. 计算各维度对目标指标的贡献度\n")
                .append("2. 使用适当的统计方法进行归因\n")
                .append("3. 处理空值和数据质量问题\n")
                .append("4. 生成清晰可维护的SQL\n");

        return context.toString();
    }

    @Data
    static class SemanticSql {
        @Description("")
        private String thought;

        @Description("sql to generate")
        private String sql;

    }

    interface SemanticSqlExtractor {
        SemanticSql generateSemanticSql(String text);
    }



    protected ChatLanguageModel getChatLanguageModel(ChatModelConfig modelConfig) {
        return ModelProvider.getChatModel(modelConfig);
    }

}
