package com.tencent.supersonic.headless.chat.parser.llm;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SimpleStrategy {

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE `test_data` ( " +
                    "`province` varchar(255) DEFAULT NULL, " +
                    "`date_id` varchar(255) DEFAULT NULL, " +
                    "`index_name` varchar(255) DEFAULT NULL, " +
                    "`index_value` varchar(255) DEFAULT NULL, " +
                    "`huanbi_value` varchar(255) DEFAULT NULL, " +
                    "`tongbi_value` varchar(255) DEFAULT NULL " +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;";

    private static final String[] PROVINCES = {
            "广东省", "江苏省", "浙江省", "山东省", "河南省", "河北省", "湖南省", "湖北省", "福建省", "上海市"
    };

    private static final String[] INDEX_NAMES = {
            "视频彩铃付费活跃用户数", "咪咕视频APP月活", "云XR（含裸眼3D）行为月活", "云游戏行为月活","GDP"
    };

    public String generatePrompt(String question) {
        // 获取当前日期并格式化为yyyy-mm-dd格式
        LocalDate currentDate = LocalDate.now();
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // 组装上下文部分
        StringBuilder context = new StringBuilder();
        // 添加SQL专家说明
        context.append("您是一个SQL专家。请帮助生成一个SQL查询以回答问题。您的回复仅应基于给定的上下文，并遵循回复指南和格式说明。\n\n");
        context.append("建表语句如下：\n").append(CREATE_TABLE_SQL).append("\n");
        context.append("表中数据的province字段包含以下省份：\n");
        for (String province : PROVINCES) {
            context.append(province).append("，");
        }
        // 删除最后一个逗号
        context.deleteCharAt(context.length() - 1);
        context.append("\n表中数据的index_name字段包含以下：\n");
        for (String index : INDEX_NAMES) {
            context.append(index).append("，");
        }
        // 删除最后一个逗号
        context.deleteCharAt(context.length() - 1);
        context.append("\n表中数据的date_id日期都是yyyy-mm-dd格式的。\n");
        context.append("当前的日期是：").append(formattedDate).append("\n");
        // 组装回复指南部分
        String replyGuideline =
                "===回复指南\n" +
                        "1. 如果提供的上下文足够，请在不附加任何解释的情况下生成一个有效的SQL查询来回答问题。\n" +
                        "2. 如果提供的上下文几乎足够，但需要了解特定列中的特定字符串的知识，请生成一个中间SQL查询以查找该列中的不同字符串。在查询前加上注释“intermediate_sql”。\n" +
                        "3. 如果提供的上下文不足，请解释为什么无法生成查询。\n" +
                        "4. 请使用最相关的表。\n" +
                        "5. 如果问题已经被问过并且回答过，请完全按照之前的回答重复答案。\n" +
                        "6. 确保输出的SQL是mysql兼容且可执行的，没有语法错误。\n";

        // 拼接完整的prompt
        return context.toString() + replyGuideline + "\n用户的问题是：" + question;
    }

//    public static void main(String[] args) {
//        SimpleStrategy simpleStrategy = new SimpleStrategy();
//        String question = "今年1月1日广东省视频彩铃付费活跃用户数的同比值是多少？";
//        String prompt = simpleStrategy.generatePrompt(question);
//        System.out.println(prompt);
//    }
}
