package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.chat.parser.llm.OnePassSCSqlGenStrategy.APP_KEY;

public class SimpleStrategy {

    public static String DIMENSION_DETECT_REGEX = "维度探测:\\[(.*?)\\]";


    public Prompt generatePrompt(LLMReq llmReq, PromptHelper promptHelper) {
        StringBuilder context = new StringBuilder();
        // 添加SQL专家说明
        context.append("您是一个SQL专家,名字叫红海ChatBI。请帮助生成一个SQL查询以回答问题。您的回复仅应基于给定的上下文，并遵循回复指南和格式说明。\n\n");
        ChatApp s2SQLParser = llmReq.getChatAppConfig().get(APP_KEY);
        if (null != s2SQLParser) {
            context.append(replaceByDimensionDetect(s2SQLParser.getPrompt(), llmReq.getSchema()))
                    .append("\n");
        }

        // 组装回复指南部分
        String replyGuideline = "===回复指南\n"
                + "1. 如果问题与表中字段和表的补充解释等数据相关，则生成有效的SQL查询来回答问题。如果问题与提供的上下文无关，请礼貌引导用户提问与当前表及数据的相关问题。例：\n"
                + "您好~这里是红海ChatBI，您的问题不在我的业务知识范围内，我可以帮你查询咪咕重点产品相关指标，比如上月咪咕视频APP活跃用户数。\n"
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

        context.append(replyGuideline).append("\n当前用户的问题是：").append(llmReq.getQueryText())
                .toString();
        // 拼接完整的prompt
        return PromptTemplate.from(String.valueOf(context)).apply(variable);
    }

    public Prompt generateStreamPrompt(LLMReq llmReq) {
        StringBuilder context = new StringBuilder();
        // 添加SQL专家说明
        context.append("您的名字叫红海ChatBI。您的回复仅应基于给定的上下文，并遵循回复指南和格式说明。\n\n");
        ChatApp s2SQLParser = llmReq.getChatAppConfig().get(APP_KEY);
        if (null != s2SQLParser) {
            context.append(s2SQLParser.getPrompt()).append("\n\n");
        }

        // 组装回复指南部分
        String replyGuideline = "===回复指南\n"
                + "1. 如果问题与表中字段和表的补充解释等数据相关，则告诉用户有关这个SQL的查询思路，结合表的元数据与查询的条件数据，仅说明中文名称不要英文字段。\n"
                + "2. 如果问题与提供的上下文无关，请礼貌引导用户提问与当前表及数据的相关问题。例：\n"
                + "您好~这里是红海ChatBI，您的问题不在我的业务知识范围内，我可以帮你查询咪咕重点产品相关指标，比如上月咪咕视频APP活跃用户数。\n"
                + "3. 只需要查询思路，不需要写出物理sql和数据库的英文字段，用中文名称代替。\n"
                + "4. 输出内容请尽量格式清晰，思路正确，字数控制在60-80字以内。\n";
        Map<String, Object> variable = new HashMap<>();
        StringBuilder exemplars = new StringBuilder();
        variable.put("exemplar", exemplars);
        context.append(replyGuideline).append("\n当前用户的问题是：").append(llmReq.getQueryText())
                .toString();
        // 拼接完整的prompt
        return PromptTemplate.from(String.valueOf(context)).apply(variable);
    }


    // 根据prompt中的维度探测文本，探测维度值，补全到prompt中
    public String replaceByDimensionDetect(String promptText, LLMReq.LLMSchema llmSchema) {
        if (llmSchema == null) {
            return promptText;
        }
        if (CollectionUtils.isEmpty(llmSchema.getValues())) {
            return promptText;
        }
        Map<String, String> fieldsMap = llmSchema.getValues().stream()
                .collect(Collectors.groupingBy(LLMReq.ElementValue::getFieldName,
                        Collectors
                                .mapping(LLMReq.ElementValue::getFieldValue, Collectors.toList())))
                .entrySet().stream()
                .collect(Collectors.toMap(stringListEntry -> stringListEntry.getKey(),
                        stringListEntry -> stringListEntry.getValue().stream()
                                .collect(Collectors.joining(","))));
        // 探测提示词文本
        Pattern pattern = Pattern.compile(DIMENSION_DETECT_REGEX);
        Matcher matcher = pattern.matcher(promptText);
        String dimensionWordsDetected = null;
        List<String> dimensionsDetected = new ArrayList<>();
        if (matcher.find()) {
            dimensionWordsDetected = matcher.group(0);
            String dimensionPart = matcher.group(1);

            // 分割维度名
            String[] dimensionArray = dimensionPart.split(",|，");
            for (String dimension : dimensionArray) {
                dimensionsDetected.add(dimension);
            }
        }
        StringBuilder replacement = new StringBuilder();
        dimensionsDetected.stream().forEach(dimension -> {
            if (fieldsMap.containsKey(dimension)) {
                replacement.append(dimension).append("条件:").append("\n");
                replacement.append(fieldsMap.get(dimension));
            }
        });
        if (null != dimensionsDetected) {
            promptText = promptText.replace(dimensionWordsDetected, replacement.toString());
        }

        return promptText;


    }

}
