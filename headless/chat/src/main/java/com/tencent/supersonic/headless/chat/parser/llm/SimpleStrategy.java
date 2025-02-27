package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SimpleStrategy {


    public String generatePrompt(LLMReq llmReq) {
        StringBuilder context = new StringBuilder();
        // 添加SQL专家说明
        context.append(
                "您是一个SQL专家,名字叫红海AI智能问答小助手。请帮助生成一个SQL查询以回答问题。您的回复仅应基于给定的上下文，并遵循回复指南和格式说明。\n\n");
        ChatApp s2SQLParser = llmReq.getChatAppConfig().get("S2SQL_PARSER");
        if (null != s2SQLParser) {
            context.append(s2SQLParser.getPrompt()).append("\n");
        }

        // 组装回复指南部分
        String replyGuideline = "===回复指南\n"
                + "1. 如果问题与表中字段和表的补充解释等数据相关，则生成有效的SQL查询来回答问题。如果问题与提供的上下文无关，请礼貌引导用户提问与当前表及数据的相关问题。例：\n"
                + "您好~这里是红海AI智能数据问答，您的问题不在我的业务知识范围内，我可以帮你查询咪咕重点产品相关指标，比如上月咪咕视频APP活跃用户数。\n"
                + "2. 如果提供的上下文足够，请在不附加任何解释的情况下生成一个有效的SQL查询来回答问题。\n"
                + "3. 确保输出的SQL是mysql兼容且可执行的，没有语法错误。\n";

        // 拼接完整的prompt
        return context.append(replyGuideline).append("\n用户的问题是：").append(llmReq.getQueryText())
                .toString();
    }

}
