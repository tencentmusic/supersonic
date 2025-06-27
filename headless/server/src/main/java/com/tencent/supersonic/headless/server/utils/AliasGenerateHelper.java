package com.tencent.supersonic.headless.server.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.service.SystemConfigService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.server.service.ModelService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AliasGenerateHelper {

    private static final int MAX_RETRIES = 3;
    private static final String ALIAS_GENERATE_FILTER_INSTRUCTION =
            "Based on the following information, your task is to select the most suitable aliases for the given element name from the candidate aliases provided. "
                    + "Please output the results as a comma-separated list.";

    private static final String VALUE_ALIAS_INSTRUCTION =
            "" + "\n#Role: You are a professional data analyst."
                    + "\n#Task: You will be provided with a json array of dimension values,"
                    + "please help generate a few aliases for each value." + "\n#Rule:"
                    + "1. ALWAYS output json object for each value."
                    + "2. The aliases should be in the same language as its original value."
                    + "\n#Exemplar:" + "Values: [\"qq_music\",\"kugou_music\"], "
                    + "Output: {\"tran\":[\"qq音乐\",\"酷狗音乐\"],"
                    + "         \"alias\":{\"qq_music\":[\"q音\",\"qq音乐\"],"
                    + "         \"kugou_music\":[\"kugou\",\"酷狗\"]}}"
                    + "\nValues: %s, Output:";

    @Autowired
    private ModelService modelService;

    public List<String> generateAlias(String modelName, String elementName, String elementType) {
        String prompt = generatePrompt(modelName, elementName, elementType);
        String llmResult = doGenerateRaw(prompt);
        if (StringUtils.isBlank(llmResult)) {
            return Lists.newArrayList();
        }
        try {
            String jsonContent = extractJsonStringFromAiMessage(llmResult);
            log.info("Extracted JSON content: {}", jsonContent);
            
            // Try to parse as simple JSON array first (expected format)
            try {
                List<String> aliases = JSON.parseObject(jsonContent, new TypeReference<List<String>>() {});
                log.info("Found aliases for element '{}': {}", elementName, aliases);
                return aliases;
            } catch (Exception e1) {
                // If that fails, try to parse as complex JSON object (fallback)
                Map<String, Object> jsonMap = JSON.parseObject(jsonContent, Map.class);
                
                // Try to find alias map from different possible field names
                Map<String, List<String>> aliasMap = null;
                if (jsonMap.containsKey("alias")) {
                    aliasMap = (Map<String, List<String>>) jsonMap.get("alias");
                }
                
                if (aliasMap != null && aliasMap.containsKey(elementName)) {
                    List<String> aliases = aliasMap.get(elementName);
                    log.info("Found aliases for element '{}': {}", elementName, aliases);
                    return aliases;
                } else {
                    log.warn("No aliases found for element '{}' in alias map: {}", elementName, aliasMap);
                    return Lists.newArrayList();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse alias json from LLM result, try to split by comma, llmResult:{}, error: {}", llmResult, e.getMessage());
            return Arrays.stream(llmResult.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }

    public List<String> filterAlias(String modelName, String elementName, String elementType,
            List<String> candidateAlias) {
        String prompt = generateFilterPrompt(modelName, elementName, elementType, candidateAlias);
        String llmResult = doGenerateRaw(prompt);
        if (StringUtils.isBlank(llmResult)) {
            return Lists.newArrayList();
        }
        return Arrays.stream(llmResult.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public String generateDimensionValueAlias(String json) {
        String prompt = String.format(VALUE_ALIAS_INSTRUCTION, json);
        return doGenerateRaw(prompt);
    }

    private String doGenerateRaw(String prompt) {
        log.info("doGenerate, prompt:{}", prompt);
        ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel();
        if (chatLanguageModel == null) {
            return "";
        }
        int maxRetries = 0;
        String llmResult = "";
        while (maxRetries < MAX_RETRIES) {
            try {
                llmResult = chatLanguageModel.generate(prompt);
                break;
            } catch (Exception e) {
                log.error("llmResult is empty, maxRetries:{}", maxRetries, e);
                maxRetries++;
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        log.info("llmResult:{}", llmResult);
        return llmResult;
    }

    private String generatePrompt(String modelName, String elementName, String elementType) {
        SystemConfigService systemConfigService = ContextUtils.getBean(SystemConfigService.class);
        String promptTemplate = systemConfigService.getSystemConfig().getAliasGeneratePrompt();
        String requirement = String.format("model name is %s, %s name is %s", modelName, elementType, elementName);
        return promptTemplate + "\n" + requirement;
    }

    private String generateFilterPrompt(String modelName, String elementName, String elementType,
            List<String> candidateAlias) {
        String requirement = String.format("model name is %s, %s name is %s, candidate aliases are %s",
                modelName, elementType, elementName, String.join(",", candidateAlias));
        return ALIAS_GENERATE_FILTER_INSTRUCTION + "\n" + requirement;
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
                new BoundaryPattern(null, null, null),
                new BoundaryPattern("```", "```", true),
                new BoundaryPattern("```json", "```", true),
                new BoundaryPattern("```JSON", "```", true),
                new BoundaryPattern("{", "}", false),
                new BoundaryPattern("[", "]", false)};
        for (BoundaryPattern pattern : patterns) {
            String extracted =
                    extractString(aiMessage, pattern.left, pattern.right, pattern.exclusionFlag);
            if (extracted == null) {
                continue;
            }
            try {
                JSON.parseObject(extracted);
                return extracted;
            } catch (JSONException ignored) {
            }
            try {
                JSON.parseArray(extracted);
                return extracted;
            } catch (JSONException ignored) {
            }
        }
        throw new JSONException("json extract failed");
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
}
