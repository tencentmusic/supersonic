package com.tencent.supersonic.headless.server.utils;

import org.junit.jupiter.api.Test;

class AliasGenerateHelperTest {

    @Test
    void extractJsonStringFromAiMessage1() {

        /** { "name": "Alice", "age": 25, "city": "New York" } */
        String testJson1 = "{\"name\": \"Alice\", \"age\": 25, \"city\": \"New York\"}";
        AliasGenerateHelper.extractJsonStringFromAiMessage(testJson1);
    }

    @Test
    void extractJsonStringFromAiMessage2() {

        /** ``` { "name": "Alice", "age": 25, "city": "New York" } ``` */
        String testJson2 = "```\n" + "{\n" + "    \"name\": \"Alice\",\n" + "    \"age\": 25,\n"
                + "    \"city\": \"New York\"\n" + "}\n" + "```";
        AliasGenerateHelper.extractJsonStringFromAiMessage(testJson2);
    }

    @Test
    void extractJsonStringFromAiMessage3() {

        /**
         * I understand that you want me to generate a JSON object with two properties: `tran` and
         * `alias`.... ```json { "name": "Alice", "age": 25, "city": "New York" } ``` Please let me
         * know if there is any problem.
         */
        String testJson3 =
                "I understand that you want me to generate a JSON object with two properties: "
                        + "`tran` and `alias`...." + "```json\n" + "{\n"
                        + "    \"name\": \"Alice\",\n" + "    \"age\": 25,\n"
                        + "    \"city\": \"New York\"\n" + "}\n" + "```"
                        + "Please let me know if there is any problem.";
        AliasGenerateHelper.extractJsonStringFromAiMessage(testJson3);
    }

    @Test
    void extractJsonStringFromAiMessage4() {

        String testJson4 =
                "Based on the provided JSON-schema, I will construct the answer as follows:\n"
                        + "\n" + "[\n" + "  \"作者名称\",\n" + "  \"作者姓名\",\n" + "  \"创作者\",\n"
                        + "  \"作者信息\"\n" + "]\n" + "\n"
                        + "This answer conforms to the format described in the JSON-schema";
        AliasGenerateHelper.extractJsonStringFromAiMessage(testJson4);
    }
}
