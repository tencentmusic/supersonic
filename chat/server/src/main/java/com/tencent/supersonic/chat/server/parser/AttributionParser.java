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
public class AttributionParser implements ChatQueryParser {
    private Boolean enableAttr = true;
    private static final Set<String> ATTRIBUTION_KEYWORDS =
            Set.of("为什么", "原因", "因素", "归因", "下降原因", "增长来源");
    public static final String APP_KEY = "S2SQL_PARSER";
    public static final String INSTRUCTION = "#Role: 资深归因分析专家\n" + "#Task: 将复杂分析拆解为多步骤SQL\n"
            + "#规则：\n" + "1. 先进行数据探查（如趋势分析）\n" + "2. 再进行维度下钻（如地区维度分解）\n" + "3. 最后计算贡献度\n"
            + "4. 每个步骤生成独立可执行的SQL\n" + "5. 不要使用with和over语法，mysql版本不支持";

    @Override
    public boolean accept(ParseContext parseContext) {
        // 1.意图识别是否是归因分析
//        IntentType intentType = parseUserIntent(parseContext.getRequest().getQueryText());
//        return intentType == IntentType.ATTRIBUTION && enableAttr;
         return false;
    }

    @Override
    public void parse(ParseContext parseContext) {
        // 2. 准备相关配置
        LLMReq llmReq = new LLMReq();

        llmReq.setQueryText(parseContext.getRequest().getQueryText());
        llmReq.setChatAppConfig(parseContext.getAgent().getChatAppConfig());
        // 3. 调用大模型生成SQL
        LLMResp llmResp = generate(llmReq);


        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode("Attribution_Analysis");
        parseInfo.setId(1);
        if (!llmResp.getSqlList().isEmpty()) {
            parseInfo.setSqlList(llmResp.getSqlList());
        } else {
            parseInfo.getSqlInfo().setCorrectedS2SQL(llmResp.getSqlOutput());
        }
        parseInfo.getSqlInfo().setResultType("text");
        parseContext.getResponse().getSelectedParses().add(parseInfo);
        parseContext.getResponse().setState(ParseResp.ParseState.COMPLETED);
    }

    private IntentType parseUserIntent(String question) {
        // TODO: 使用LLM解析用户查询意图
        // 使用大模型进行意图分类
        // String prompt = "判断问题类型：\n问题：" + question + "\n选项：数据查询、归因分析";
        // String response = LLMClient.query(prompt);
        // return response.contains("归因分析") ?
        // IntentType.ATTRIBUTION : IntentType.NORMAL_QUERY;
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

    private List<String> generateAttributionSql(LLMReq llmReq) {
        // 1. 获取基础信息
        ChatApp chatApp = llmReq.getChatAppConfig().get(APP_KEY);
        // 2. 构建归因分析提示语
        String prompt = generatePrompt(llmReq, chatApp);

        // 3. 调用LLM生成SQL
        ChatLanguageModel model = getChatLanguageModel(chatApp.getChatModelConfig());
        SemanticSqlExtractor extractor = AiServices.create(SemanticSqlExtractor.class, model);

        // 4. 解析LLM返回结果
        SemanticAnalysisSteps steps = extractor.generateSemanticSql(prompt);
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
            String cleanSql = matcher.group(2).replaceAll("\\s+", " ").replaceAll(";$", "").trim();
            steps.add(cleanSql);
        }

        // 保底处理：分号分割
        if (steps.isEmpty()) {
            steps = Arrays.stream(stepsStr.split(";")).filter(StringUtils::isNotBlank)
                    .map(String::trim).collect(Collectors.toList());
        }

        return steps;
    }
    private String generatePrompt(LLMReq llmReq, ChatApp chatApp) {
        StringBuilder context = new StringBuilder();
//        context.append(INSTRUCTION.replace("单一SQL", "多步骤SQL"))
//                .append("\n\n新增规则：")
//                .append("\n7. 将复杂问题拆分为多个分析步骤")
//                .append("\n8. 每个步骤生成简单且独立的SQL")
//                .append("\n9. 明确步骤间的逻辑顺序")
//                .append("\n10. 最终给出汇总逻辑\n\n");
//        if (chatApp != null) {
//            String fullPrompt = chatApp.getPrompt();
//            // 查找“示例：”在字符串中的位置
//            int exampleIndex = fullPrompt.indexOf("示例：");
//
//            // 如果找到“示例：”，则截取该位置之前的内容
//            if (exampleIndex != -1) {
//                context.append("### 业务背景信息：\n")
//                        .append(fullPrompt.substring(0, exampleIndex).trim()).append("\n\n");
//            }else {
//                // 如果没有找到“示例：”，则返回完整内容
//                context.append("### 业务背景信息：\n").append(fullPrompt).append("\n\n");
//            }
//        }
//        context.append("\n请严格按以下格式返回：")
//                .append("\n思考过程：<分析思路>")
//                .append("\n步骤1-SQL: SELECT...;")
//                .append("\n步骤2-SQL: SELECT...;")
//                .append("\n汇总逻辑：<结果组合方式>")
//                .append("\n\n示例：")
//                .append("\n思考过程：先分析整体趋势，再拆解维度")
//                .append("\n步骤1-SQL: SELECT date, SUM(sales) FROM tbl GROUP BY date;")
//                .append("\n步骤2-SQL: SELECT region, SUM(sales) FROM tbl GROUP BY region;")
//                .append("\n汇总逻辑：结合时间和地域维度分析销售变化");

        context.append("#Role: 资深归因分析专家\n" +
                "#Task: \n" +
                "1.根据数据情况与问题，拆解成多个sql语句查询各方面的数据\n" +
                "2.根据各方面的数据结果，分析原因，形成分析报告\n" +
                "#规则：\n" +
                "1. 将复杂问题拆分为多个分析步骤\n" +
                "2. 每个步骤生成独立可执行的SQL\n" +
                "3. 明确步骤间的逻辑顺序\n" +
                "\n" +
                "\n" +
                "### 业务背景信息：\n" +
                "\n" +
                "渠道场景库：\n" +
                "CREATE TABLE hhapp_2025_chatbi_active1_m(\n" +
                "  province_name   COMMENT 省份, \n" +
                " period_id  COMMENT 日期,\n" +
                "data_type COMMENT 日期类型,\n" +
                "product_name   COMMENT 产品名称,\n" +
                " channel_level2 COMMENT 渠道 ,\n" +
                " pid2_2022 COMMENT 场景,\n" +
                " active_users COMMENT 活跃用户数,\n" +
                " idx_mm_rate COMMENT 环比\n" +
                ") \n" +
                "\n" +
                "\n" +
                "# 维度可选值如下，所有的维度查询，必须命中如下维度中的可选值，不能随意发挥。\n" +
                "1.以上表的省份维度：\n" +
                "-可选值：湖南，安徽，广西，山东，河北，天津，新疆，上海，浙江，江苏，未知，辽宁，四川，青海，云南，宁夏，广东，贵州，黑龙江，海南，西藏，吉林，湖北，福建，陕西，甘肃，北京，内蒙古，重庆，山西，江西，河南，全国\n" +
                "2.渠道场景库产品维度：\n" +
                "-可选值：咪咕视频,咪咕阅读,云游戏,咪咕音乐\n" +
                "3.渠道场景库的渠道维度：\n" +
                "-可选值：终端预装,运营类渠道,公共池,复合型渠道,省专渠道,互联网公域流量运营,广宣及MCN,外部导流\n" +
                "4.渠道场景库的场景维度:\n" +
                "-可选值：APP,小程序,家庭,SDK,网页/H5,公网,政企,其他\n" +
                "\n" +
                "# 查询规则\n" +
                "[基本规则]\n" +
                "所有查询都只能在单个表里进行，不能进行跨表，请根据mysql5.7版本生成查询语句\n" +
                "\n" +
                "[渠道场景库规则]\n" +
                "表名：hhapp_2025_chatbi_active1_m\n" +
                "渠道/场景处理：\n" +
                "√ 未提及维度 → 赋\"全部\"\n" +
                "√ 具体值 → 精准匹配\n" +
                "√ \"分渠道/分场景\" → 维度!='全部'\n" +
                "√ TOP N查询 → 自动排除\"全部\"值\n" +
                "[时间规则]\n" +
                "√渠道场景库的日期字段包含月(yyyyMM)和日(yyyyMMdd)两种格式，查询日数据需要指定日期类型字段取值为day,查询月数据指定日期类型字段取值为month：\n" +
                "√渠道场景库未指定日期/提及当前/最新/最近时，默认查上月。当提及年时按月查询，当提及周时按日查询\n" +
                "√今年=2025(未指定年时，默认使用,去年=当前年-1,例如去年10月为202410，今年1月为202501\n" +
                "[优先级规则]\n" +
                "\n" +
                "请严格按以下格式返回：\n" +
                "思考过程：\n" +
                "<思路1的查询SQL>: SELECT...;\n" +
                "<思路2的查询SQL>: SELECT...;");
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
