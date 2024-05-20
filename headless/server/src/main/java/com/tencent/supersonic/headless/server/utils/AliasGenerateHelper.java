package com.tencent.supersonic.headless.server.utils;


import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class AliasGenerateHelper {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    public String getChatCompletion(String message) {
        SystemMessage from = SystemMessage.from(message);
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
                + "ANSWER JSON:";
        log.info("msg:{}", msg);
        return getChatCompletion(msg);
    }

    public String generateDimensionValueAlias(String json) {
        String msg = "Assuming you are a professional data analyst specializing in indicators,for you a json list，"
                + "the required content to follow is as follows: "
                + "1. The format of JSON,"
                + "2. Only return in JSON format,"
                + "3. the array item > 1 and < 5,more alias,"
                + "for example：input:[\"qq_music\",\"kugou_music\"],"
                + "out:{\"tran\":[\"qq音乐\",\"酷狗音乐\"],\"alias\":{\"qq_music\":[\"q音\",\"qq音乐\"],"
                + "\"kugou_music\":[\"kugou\",\"酷狗\"]}},"
                + "now input: "
                + json + ","
                + "answer json:";
        log.info("msg:{}", msg);
        return getChatCompletion(msg);
    }
}
