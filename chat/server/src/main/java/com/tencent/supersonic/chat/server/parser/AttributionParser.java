package com.tencent.supersonic.chat.server.parser;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.tencent.supersonic.chat.api.pojo.enums.IntentType;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import jdk.jfr.Description;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class AttributionParser implements ChatQueryParser{
    private Boolean enableAttr = false;
    private static final Set<String> ATTRIBUTION_KEYWORDS =
            Set.of("为什么", "原因", "因素", "归因", "下降原因", "增长来源");
    public static final String APP_KEY = "S2SQL_PARSER";
    public static final String INSTRUCTION =
            "#Role: 资深归因分析专家\n" +
                    "#Task: 将复杂分析拆解为多步骤SQL\n" +
                    "#规则：\n" +
                    "1. 先进行数据探查（如趋势分析）\n" +
                    "2. 再进行维度下钻（如地区维度分解）\n" +
                    "3. 最后计算贡献度\n" +
                    "4. 每个步骤生成独立可执行的SQL\n" +
                    "5. 步骤间通过中间结果关联\n" +
                    "6. 使用临时表需命名规范（tmp_step1）\n" +
                    "7. 不要使用with和over语法，mysql版本不支持";
    @Override
    public boolean accept(ParseContext parseContext) {
        //1.意图识别是否是归因分析
        IntentType intentType = parseUserIntent(parseContext.getRequest().getQueryText());
        if (intentType == IntentType.ATTRIBUTION && enableAttr) {
            return true;
        }
        return true;
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
            List<String> attributionSql = generateAttributionSql(llmReq);
            llmResp.setSqlList(attributionSql);
        } catch (Exception e) {
            log.error("归因分析SQL生成失败", e);
            throw new RuntimeException("归因分析SQL生成失败", e);
        }
        return llmResp;
    }
    private  List<String> generateAttributionSql(LLMReq llmReq) {
        // 1. 获取基础信息
        ChatApp chatApp = llmReq.getChatAppConfig().get(APP_KEY);
        // 2. 构建归因分析提示语
        String prompt = generatePrompt(llmReq, chatApp);

        // 3. 调用LLM生成SQL
        ChatLanguageModel model = getChatLanguageModel(chatApp.getChatModelConfig());
        SemanticSqlExtractor extractor = AiServices.create(SemanticSqlExtractor.class, model);

        // 4. 解析LLM返回结果
        SemanticAnalysisSteps steps  = extractor.generateSemanticSql(prompt);
        if (StringUtils.isBlank(steps.getSqlStepsStr())) {
            throw new RuntimeException("未生成有效分析步骤");
        }

        List<String> sqlSteps = parseSqlSteps(steps.getSqlStepsStr());

        // 有效性校验
        if (sqlSteps.stream().anyMatch(s -> !s.toLowerCase().startsWith("select"))) {
            throw new RuntimeException("包含非查询语句：" + steps.getSqlStepsStr());
        }

        return sqlSteps;

    }
    private List<String> parseSqlSteps(String stepsStr) {
        List<String> steps = new ArrayList<>();

        // 使用正则匹配带序号的步骤
        Pattern pattern = Pattern.compile("步骤(\\d+)-SQL:\\s*((?:[^;]|(?:'.*?'))*?);");
        Matcher matcher = pattern.matcher(stepsStr.replace("\n", " "));

        while (matcher.find()) {
            String cleanSql = matcher.group(2)
                    .replaceAll("\\s+", " ")  // 压缩空格
                    .replaceAll(";$", "")     // 去除末尾分号
                    .trim();
            steps.add(cleanSql);
        }

        // 保底处理：分号分割
        if (steps.isEmpty()) {
            steps = Arrays.stream(stepsStr.split(";"))
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        return steps;
    }
    private String generatePrompt(LLMReq llmReq, ChatApp chatApp) {
        StringBuilder context = new StringBuilder();
        context.append(INSTRUCTION.replace("单一SQL", "多步骤SQL"))
                .append("\n\n新增规则：")
                .append("\n7. 将复杂问题拆分为多个分析步骤")
                .append("\n8. 每个步骤生成简单且独立的SQL")
                .append("\n9. 明确步骤间的逻辑顺序")
                .append("\n10. 最终给出汇总逻辑\n\n");
        if (chatApp != null) {
            String fullPrompt = chatApp.getPrompt();
            // 查找“示例：”在字符串中的位置
            int exampleIndex = fullPrompt.indexOf("示例：");

            // 如果找到“示例：”，则截取该位置之前的内容
            if (exampleIndex != -1) {
                context.append("### 业务背景信息：\n")
                        .append(fullPrompt.substring(0, exampleIndex).trim()).append("\n\n");
            }else {
                // 如果没有找到“示例：”，则返回完整内容
                context.append("### 业务背景信息：\n").append(fullPrompt).append("\n\n");
            }
        }
        context.append("\n请严格按以下格式返回：")
                .append("\n思考过程：<分析思路>")
                .append("\n步骤1-SQL: SELECT...;")
                .append("\n步骤2-SQL: SELECT...;")
                .append("\n汇总逻辑：<结果组合方式>")
                .append("\n\n示例：")
                .append("\n思考过程：先分析整体趋势，再拆解维度")
                .append("\n步骤1-SQL: SELECT date, SUM(sales) FROM tbl GROUP BY date;")
                .append("\n步骤2-SQL: SELECT region, SUM(sales) FROM tbl GROUP BY region;")
                .append("\n汇总逻辑：结合时间和地域维度分析销售变化");

        return context.toString();
    }

    @Data
    static class SemanticAnalysisSteps {
        @Description("分析步骤思考过程")
        private String analysisThought;

        @Description("多步骤SQL字符串，格式：步骤1-SQL:{{sql}};步骤2-SQL:{{sql}};...")
        private String sqlStepsStr;

        @Description("最终汇总逻辑描述")
        private String summaryLogic;
    }

    interface SemanticSqlExtractor {
        @UserMessage("根据业务背景和问题生成多步骤分析，按指定格式返回. {{it}}")
        SemanticAnalysisSteps generateSemanticSql(String text);
    }



    protected ChatLanguageModel getChatLanguageModel(ChatModelConfig modelConfig) {
        return ModelProvider.getChatModel(modelConfig);
    }

}
