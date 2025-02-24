package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SimpleStrategy {


    public String generatePrompt(LLMReq llmReq) {
        StringBuilder context = new StringBuilder();
        // 添加SQL专家说明
        context.append("您是一个SQL专家。请帮助生成一个SQL查询以回答问题。您的回复仅应基于给定的上下文，并遵循回复指南和格式说明。\n\n");
        ChatApp s2SQLParser = llmReq.getChatAppConfig().get("S2SQL_PARSER");
        if(null!=s2SQLParser){
            context.append(s2SQLParser.getPrompt()).append("\n");
        }

        // 组装回复指南部分
        String replyGuideline = "===回复指南\n" + "1. 如果提供的上下文足够，请在不附加任何解释的情况下生成一个有效的SQL查询来回答问题。\n"
                + "2. 如果提供的上下文几乎足够，但需要了解特定列中的特定字符串的知识，请生成一个中间SQL查询以查找该列中的不同字符串。在查询前加上注释“intermediate_sql”。\n"
                + "3. 如果提供的上下文不足，请解释为什么无法生成查询。\n" + "4. 请使用最相关的表。\n"
                + "5. 如果问题已经被问过并且回答过，请完全按照之前的回答重复答案。\n" + "6. 确保输出的SQL是mysql兼容且可执行的，没有语法错误。\n";

        // 拼接完整的prompt
        return context.append(replyGuideline).append("\n用户的问题是：").append(llmReq.getQueryText()).toString();
    }

}
