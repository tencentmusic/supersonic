package com.tencent.supersonic.headless.chat.parser.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class InputFormat {

    public static final String SEPERATOR = "\n\n";

    public static String format(String template, List<String> templateKey,
            List<Map<String, String>> toFormatList) {
        List<String> result = new ArrayList<>();

        for (Map<String, String> formatItem : toFormatList) {
            Map<String, String> retrievalMeta = subDict(formatItem, templateKey);
            result.add(format(template, retrievalMeta));
        }

        return String.join(SEPERATOR, result);
    }

    public static String format(String input, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            input = input.replace(entry.getKey(), entry.getValue());
        }
        return input;
    }

    private static Map<String, String> subDict(Map<String, String> dict, List<String> keys) {
        Map<String, String> subDict = new HashMap<>();
        for (String key : keys) {
            if (dict.containsKey(key)) {
                subDict.put(key, dict.get(key));
            }
        }
        return subDict;
    }
}