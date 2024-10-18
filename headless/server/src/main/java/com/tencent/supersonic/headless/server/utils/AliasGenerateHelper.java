package com.tencent.supersonic.headless.server.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AliasGenerateHelper {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    private static final String NAME_ALIAS_INSTRUCTION = ""
            + "#Role: You are a professional data analyst specializing in metrics and dimensions."
            + "\n#Task: You will be provided with metadata about a metric or dimension, please help "
            + "generate a few aliases in the same language as its `fieldName`." + "\n#Rules:"
            + "1. Please do not generate aliases like xxx1, xxx2, xxx3."
            + "2. Please do not generate aliases that are the same as the original names of metrics/dimensions."
            + "3. Please pay attention to the quality of the generated aliases and "
            + "avoid creating aliases that look like test data."
            + "4. Please output as a json string array."
            + "\n#Metadata: {'table':'{{table}}', 'name':'{{name}}', 'type':'{{type}}', "
            + "'field':'field', 'description':'{{desc}}'}" + "\n#Output:";

    private static final String VALUE_ALIAS_INSTRUCTION =
            "" + "\n#Role: You are a professional data analyst."
                    + "\n#Task: You will be provided with a json array of dimension values,"
                    + "please help generate a few aliases for each value." + "\n#Rule:"
                    + "1. ALWAYS output json array for each value."
                    + "2. The aliases should be in the same language as its original value."
                    + "\n#Exemplar:" + "Values: [\\\"qq_music\\\",\\\"kugou_music\\\"], "
                    + "Output: {\\\"tran\\\":[\\\"qq音乐\\\",\\\"酷狗音乐\\\"],"
                    + "         \\\"alias\\\":{\\\"qq_music\\\":[\\\"q音\\\",\\\"qq音乐\\\"],"
                    + "         \\\"kugou_music\\\":[\\\"kugou\\\",\\\"酷狗\\\"]}}"
                    + "\nValues: {{values}}, Output:";

    public String generateAlias(String mockType, String name, String bizName, String table,
            String desc) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("table", table);
        variable.put("name", name);
        variable.put("field", bizName);
        variable.put("type", mockType);
        variable.put("desc", desc);

        Prompt prompt = PromptTemplate.from(NAME_ALIAS_INSTRUCTION).apply(variable);
        String response = getChatCompletion(prompt);
        keyPipelineLog.info("AliasGenerateHelper.generateAlias modelReq:\n{} \nmodelResp:\n{}",
                prompt.text(), response);
        return response;
    }

    public String generateDimensionValueAlias(String json) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("values", json);

        Prompt prompt = PromptTemplate.from(VALUE_ALIAS_INSTRUCTION).apply(variable);
        String response = getChatCompletion(prompt);
        keyPipelineLog.info(
                "AliasGenerateHelper.generateValueAlias modelReq:\n{} " + "\nmodelResp:\n{}",
                prompt.text(), response);


        return response;
    }

    private String getChatCompletion(Prompt prompt) {
        SystemMessage from = prompt.toSystemMessage();
        ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel();
        Response<AiMessage> response = chatLanguageModel.generate(from);
        return response.content().text();
    }

    private static String extractString(String targetString, String left, String right,
            Boolean exclusionFlag) {
        if (targetString == null || left == null || right == null || exclusionFlag == null) {
            return targetString;
        }
        if (left.equals(right)) {
            int firstIndex = targetString.indexOf(left);
            if (firstIndex == -1) {
                return null;
            }
            int secondIndex = targetString.indexOf(left, firstIndex + left.length());
            if (secondIndex == -1) {
                return null;
            }
            String extractedString =
                    targetString.substring(firstIndex + left.length(), secondIndex);
            if (!exclusionFlag) {
                extractedString = left + extractedString + right;
            }
            return extractedString;
        } else {
            int leftIndex = targetString.indexOf(left);
            if (leftIndex == -1) {
                return null;
            }
            int start = leftIndex + left.length();
            int rightIndex = targetString.indexOf(right, start);
            if (rightIndex == -1) {
                return null;
            }
            String extractedString = targetString.substring(start, rightIndex);
            if (!exclusionFlag) {
                extractedString = left + extractedString + right;
            }
            return extractedString;
        }
    }

    public static String extractJsonStringFromAiMessage(String aiMessage) {
        class BoundaryPattern {
            final String left;
            final String right;
            final Boolean exclusionFlag;

            public BoundaryPattern(String start, String end, Boolean includeMarkers) {
                this.left = start;
                this.right = end;
                this.exclusionFlag = includeMarkers;
            }
        }
        BoundaryPattern[] patterns = {
                        // 不做任何匹配
                        new BoundaryPattern(null, null, null),
                        // ```{"name":"Alice","age":25,"city":"NewYork"}```
                        new BoundaryPattern("```", "```", true),
                        // ```json {"name":"Alice","age":25,"city":"NewYork"}```
                        new BoundaryPattern("```json", "```", true),
                        // ```JSON {"name":"Alice","age":25,"city":"NewYork"}```
                        new BoundaryPattern("```JSON", "```", true),
                        // {"name":"Alice","age":25,"city":"NewYork"}
                        new BoundaryPattern("{", "}", false),
                        // ["Alice", "Bob"]
                        new BoundaryPattern("[", "]", false)};
        for (BoundaryPattern pattern : patterns) {
            String extracted =
                    extractString(aiMessage, pattern.left, pattern.right, pattern.exclusionFlag);
            if (extracted == null) {
                continue;
            }
            // 判断是否能解析为Object或者Array
            try {
                JSON.parseObject(extracted);
                return extracted;
            } catch (JSONException ignored) {
                // ignored
            }
            try {
                JSON.parseArray(extracted);
                return extracted;
            } catch (JSONException ignored) {
                // ignored
            }
        }
        throw new JSONException("json extract failed");
    }
}
