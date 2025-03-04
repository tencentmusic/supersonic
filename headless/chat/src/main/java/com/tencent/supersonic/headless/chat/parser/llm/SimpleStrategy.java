package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.tencent.supersonic.headless.chat.parser.llm.OnePassSCSqlGenStrategy.APP_KEY;

public class SimpleStrategy {


    public Prompt generatePrompt(LLMReq llmReq, PromptHelper promptHelper) {
        StringBuilder context = new StringBuilder();
        // 添加SQL专家说明
        context.append(
                "您是一个SQL专家,名字叫红海AI智能问答小助手。请帮助生成一个SQL查询以回答问题。您的回复仅应基于给定的上下文，并遵循回复指南和格式说明。\n\n");
        ChatApp s2SQLParser = llmReq.getChatAppConfig().get(APP_KEY);
        if (null != s2SQLParser) {
            context.append(s2SQLParser.getPrompt()).append("\n\n");
        }
//        if (Objects.nonNull(llmReq.getSchema()) && StringUtils.equalsIgnoreCase(llmReq.getSchema().getDataSetName(), "红海app数据集直连模式")) {
//            String dataSemantics = promptHelper.buildSchemaStr(llmReq);
//            context.append("schema:").append("\n");
//            context.append(dataSemantics).append("\n");
//        }
        // 组装回复指南部分
        String replyGuideline = "===回复指南\n"
                + "1. 如果问题与表中字段和表的补充解释等数据相关，则生成有效的SQL查询来回答问题。如果问题与提供的上下文无关，请礼貌引导用户提问与当前表及数据的相关问题。例：\n"
                + "您好~这里是红海AI智能数据问答，您的问题不在我的业务知识范围内，我可以帮你查询咪咕重点产品相关指标，比如上月咪咕视频APP活跃用户数。\n"
                + "2. 如果提供的上下文足够，请在不附加任何解释的情况下生成一个有效的SQL查询来回答问题。\n"
                + "3. 确保输出的SQL是mysql兼容且可执行的，没有语法错误。\n";
        StringBuilder exemplars = new StringBuilder();
        if (Objects.nonNull(llmReq.getDynamicExemplars())) {
            for (Text2SQLExemplar exemplar : llmReq.getDynamicExemplars()) {
                String exemplarStr = String.format("\nQuestion:%s,Schema:%s,SideInfo:%s,SQL:%s",
                        exemplar.getQuestion(), exemplar.getDbSchema(), exemplar.getSideInfo(),
                        exemplar.getSql());
                exemplars.append(exemplarStr);
            }
        }

        Map<String, Object> variable = new HashMap<>();
        variable.put("exemplar", exemplars);

        context.append(replyGuideline).append("\n当前用户的问题是：").append(llmReq.getQueryText()).toString();
        // 拼接完整的prompt
        return PromptTemplate.from(String.valueOf(context)).apply(variable);
    }

    public Prompt generateStreamPrompt(LLMReq llmReq) {
        StringBuilder context = new StringBuilder();
        // 添加SQL专家说明
        context.append(
                "您是一个SQL专家,名字叫红海AI智能问答小助手。您的回复仅应基于给定的上下文，并遵循回复指南和格式说明。\n\n");
        ChatApp s2SQLParser = llmReq.getChatAppConfig().get(APP_KEY);
        if (null != s2SQLParser) {
            context.append(s2SQLParser.getPrompt()).append("\n\n");
        }

        // 组装回复指南部分
        String replyGuideline = "===回复指南\n"
                + "1. 如果问题与表中字段和表的补充解释等数据相关，则告诉用户有关这个SQL的查询思路，结合表的元数据与查询的条件数据。如果问题与提供的上下文无关，请礼貌引导用户提问与当前表及数据的相关问题。例：\n"
                + "您好~这里是红海AI智能数据问答，您的问题不在我的业务知识范围内，我可以帮你查询咪咕重点产品相关指标，比如上月咪咕视频APP活跃用户数。\n"
                + "2. 只需要查询思路，不需要写出实际的sql。\n";
        Map<String, Object> variable = new HashMap<>();
        StringBuilder exemplars = new StringBuilder();
        variable.put("exemplar", exemplars);
        context.append(replyGuideline).append("\n当前用户的问题是：").append(llmReq.getQueryText()).toString();
        // 拼接完整的prompt
        return PromptTemplate.from(String.valueOf(context)).apply(variable);
    }

}
