package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.commons.lang.time.DateFormatUtils;
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
                + "您好~这里是红海ChatBI，您的问题不在我的业务知识范围内，我可以帮您查询咪咕重点产品的核心指标数据、分省、分渠道、分场景的活跃数据，咪咕视频的内容播放数据，比如您可以查询咪咕视频上月的全场景活跃用户，最近一周最火的体育赛事。\n"
                + "2. 如果提供的上下文足够，请在不附加任何解释的情况下生成一个有效的SQL查询来回答问题。\n"
                + "3. 确保输出的SQL是mysql兼容且可执行的，没有语法错误。\n"
                + "4. 为了防止输出的SQL在使用后返回数据量太大，确保输出的SQL都是限制了最大返回条数的，按照用户问题最后生成的SQL没有LIMIT结尾的时候，要求生成的SQL的最后必须加上LIMIT 100，按照用户问题最后生成的SQL有LIMIT结尾的时候不用再加LIMIT，没有语法错误。\n";

        StringBuilder exemplars = new StringBuilder();
        if (Objects.nonNull(llmReq.getDynamicExemplars())) {
            for (Text2SQLExemplar exemplar : llmReq.getDynamicExemplars()) {
                String exemplarStr =
                        String.format("问题:%s\n回答:%s\n", exemplar.getQuestion(), exemplar.getSql());
                exemplars.append(exemplarStr);
            }
        }

        Map<String, Object> variable = new HashMap<>();
        variable.put("exemplar-recall", exemplars);

        String currentDayRule = new StringBuilder("所有日期不用日期函数，根据今天的日期去推算过去，今天的日期是")
                .append(DateFormatUtils.format(new Date(), "yyyyMMdd")).append("\n").toString();
        variable.put("current-day-rule", currentDayRule);

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
            String fullPrompt = s2SQLParser.getPrompt();
            context.append("### 业务背景信息：\n").append(fullPrompt).append("\n\n");
        }

        // 组装回复指南部分
        String replyGuideline = "===回复指南\n"
                + "1. 如果用户的问题与业务背景信息相关，则展示当前用户问题的查询思考思路，结合表的元数据与查询的条件数据，仅说明中文名称不要英文字段。\n"
                + "2. 如果用户的问题与业务背景信息无关，请礼貌引导用户提问与当前表及数据的相关问题。例：\n"
                + "您好~这里是红海ChatBI，您的问题不在我的业务知识范围内，我可以帮您查询咪咕重点产品的核心指标数据、分省、分渠道、分场景的活跃数据，咪咕视频的内容播放数据，比如您可以查询咪咕视频上月的全场景活跃用户，最近一周最火的体育赛事。\n"
                + "3. 只需要查询思考思路，**严格禁止在思考过程中出现任何SQL代码片段或英文字段名**，必须使用中文描述查询逻辑。\n"
                + "4. 输出内容请尽量格式清晰，思路正确，字数控制在80-100字左右。\n"
                + "5. 如果问题提及集团考核/考核/考核指标，那么在输出的思路中增加提示：\"暂时以24年考核目标作为参照，待25年考核指标下达后再更新\",其他任何情况请不要添加如上的提示\n";



        Map<String, Object> variable = new HashMap<>();
        StringBuilder exemplars = new StringBuilder();
        variable.put("exemplar", exemplars);
        variable.put("question", llmReq.getQueryText());
        variable.put("schema", "");
        variable.put("information", "");
        String currentDayRule = new StringBuilder("所有日期不用日期函数，根据今天的日期去推算过去，今天的日期是")
                .append(DateFormatUtils.format(new Date(), "yyyyMMdd")).append("\n").toString();
        variable.put("current-day-rule", currentDayRule);
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
        if (!CollectionUtils.isEmpty(dimensionsDetected)) {
            promptText = promptText.replace(dimensionWordsDetected, replacement.toString());
        }

        return promptText;


    }

}
