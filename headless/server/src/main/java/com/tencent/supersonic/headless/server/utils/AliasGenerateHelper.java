package com.tencent.supersonic.headless.server.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class AliasGenerateHelper {

    public String getChatCompletion(String message) {
        SystemMessage from = SystemMessage.from(message);
        ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel();
        Response<AiMessage> response = chatLanguageModel.generate(from);
        log.info("message:{}\n response:{}", message, response);
        return response.content().text();
    }

    public String generateAlias(String mockType,
                                String name,
                                String bizName,
                                String table,
                                String desc,
                                Boolean isPercentage) {
        String msg = "Assuming you are a professional data analyst specializing in metrics and dimensions, "
                + "you have a vast amount of data analysis metrics content. You are familiar with the basic"
                + " format of the content,Now, Construct your answer Based on the following json-schema.\n"
                + "{\n"
                + "\"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "\"type\": \"array\",\n"
                + "\"minItems\": 2,\n"
                + "\"maxItems\": 4,\n"
                + "\"items\": {\n"
                + "\"type\": \"string\",\n"
                + "\"description\": \"Assuming you are a data analyst and give a defined "
                + mockType
                + " name: "
                + name + ","
                + "this "
                + mockType
                + " is from database and table: "
                + table + ",This "
                + mockType
                + " calculates the field source: "
                + bizName
                + ", The description of this metrics is: "
                + desc
                + ", provide some aliases for this, please take chinese or english,"
                + "You must adhere to the following rules:\n"
                + "1. Please do not generate aliases like xxx1, xxx2, xxx3.\n"
                + "2. Please do not generate aliases that are the same as the original names of metrics/dimensions.\n"
                + "3. Please pay attention to the quality of the generated aliases and "
                + "   avoid creating aliases that look like test data.\n"
                + "4. Please generate more Chinese aliases."
                + "},\n"
                + "\"additionalProperties\":false}\n"
                + "Please double-check whether the answer conforms to the format described in the JSON-schema.\n"
                + "回答格式示例:"
                + "[\n"
                + "  \"人数\",\n"
                + "  \"员工人数\",\n"
                + "  \"员工数量\",\n"
                + "  \"员工总数\"\n"
                + "]\n"
                + "请严格按照示例格式进行生成。"
                + "ANSWER JSON:";
        log.info("msg:{}", msg);
        return getChatCompletion(msg);
    }

    public String generateDimensionValueAlias(String json) {
        String msg = "Assuming you are a professional data analyst specializing in indicators,for you a json list，"
                + "the required content to follow is as follows: \n"
                + "1. The format of JSON,\n"
                + "2. Only return in JSON format,\n"
                + "3. the array item > 1 and < 5,more alias,\n"
                + "for example：\n"
                + "input:[\"qq_music\",\"kugou_music\"],\n"
                + "out:{\"tran\":[\"qq音乐\",\"酷狗音乐\"],"
                + "\"alias\":{\"qq_music\":[\"q音\",\"qq音乐\"],\"kugou_music\":[\"kugou\",\"酷狗\"]}},\n"
                + "input:[\"qq_music\",\"kugou_music\"],\n"
                + "out:{\"tran\":[\"qq音乐\",\"酷狗音乐\"],"
                + "\"alias\":{\"qq_music\":[\"q音\",\"qq音乐\"],\"kugou_music\":[\"kugou\",\"酷狗\"]}},\n"
                + "input:[\"大专\",\"本科\",\"硕士研究生\"],\n"
                + "out:{\"tran\":[\"大专\",\"本科\",\"硕士研究生\"],"
                + "\"alias\":{\"大专\":[\"专科\",\"大学专科\"],\"本科\":[\"学士\",\"本科生\"],\"硕士研究生\":[\"硕士\",\"研究生\"]}},\n"
                + "now input: "
                + json + ",\n"
                + "answer json:";
        log.info("msg:{}", msg);
        return getChatCompletion(msg);
    }

    private static String extractString(String targetString, String left, String right, Boolean exclusionFlag) {
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
            String extractedString = targetString.substring(firstIndex + left.length(), secondIndex);
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
                //不做任何匹配
                new BoundaryPattern(null, null, null),
                //```{"name":"Alice","age":25,"city":"NewYork"}```
                new BoundaryPattern("```", "```", true),
                //```json {"name":"Alice","age":25,"city":"NewYork"}```
                new BoundaryPattern("```json", "```", true),
                //```JSON {"name":"Alice","age":25,"city":"NewYork"}```
                new BoundaryPattern("```JSON", "```", true),
                //{"name":"Alice","age":25,"city":"NewYork"}
                new BoundaryPattern("{", "}", false),
                //["Alice", "Bob"]
                new BoundaryPattern("[", "]", false)
        };
        for (BoundaryPattern pattern : patterns) {
            String extracted = extractString(aiMessage, pattern.left, pattern.right, pattern.exclusionFlag);
            if (extracted == null) {
                continue;
            }
            //判断是否能解析为Object或者Array
            try {
                JSON.parseObject(extracted);
                return extracted;
            } catch (JSONException ignored) {
                //ignored
            }
            try {
                JSON.parseArray(extracted);
                return extracted;
            } catch (JSONException ignored) {
                //ignored
            }
        }
        throw new JSONException("json extract failed");
    }
}
