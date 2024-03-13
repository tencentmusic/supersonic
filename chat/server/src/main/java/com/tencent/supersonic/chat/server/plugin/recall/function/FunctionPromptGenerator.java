package com.tencent.supersonic.chat.server.plugin.recall.function;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.chat.server.plugin.PluginParseConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.chat.parser.llm.InputFormat;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FunctionPromptGenerator {

    public String generateFunctionCallPrompt(String queryText, List<PluginParseConfig> toolConfigList) {
        List<String> toolExplainList = toolConfigList.stream()
                .map(this::constructPluginPrompt)
                .collect(Collectors.toList());
        String functionList = String.join(InputFormat.SEPERATOR, toolExplainList);
        return constructTaskPrompt(queryText, functionList);
    }

    public String constructPluginPrompt(PluginParseConfig parseConfig) {
        String toolName = parseConfig.getName();
        String toolDescription = parseConfig.getDescription();
        List<String> toolExamples = parseConfig.getExamples();

        StringBuilder prompt = new StringBuilder();
        prompt.append("【工具名称】\n").append(toolName).append("\n");
        prompt.append("【工具描述】\n").append(toolDescription).append("\n");
        prompt.append("【工具适用问题示例】\n");
        for (String example : toolExamples) {
            prompt.append(example).append("\n");
        }
        return prompt.toString();
    }

    public String constructTaskPrompt(String queryText, String functionList) {
        String instruction = String.format("问题为:%s\n请根据问题和工具的描述，选择对应的工具，完成任务。"
                + "请注意，只能选择1个工具。请一步一步地分析选择工具的原因(每个工具的【工具适用问题示例】是选择的重要参考依据)，"
                + "并给出最终选择，输出格式为json,key为’分析过程‘, ’选择工具‘", queryText);

        return String.format("工具选择如下:\n\n%s\n\n【任务说明】\n%s", functionList, instruction);
    }

    public FunctionResp requestFunction(FunctionReq functionReq) {

        FunctionPromptGenerator promptGenerator = ContextUtils.getBean(FunctionPromptGenerator.class);

        ChatLanguageModel chatLanguageModel = ContextUtils.getBean(ChatLanguageModel.class);
        String functionCallPrompt = promptGenerator.generateFunctionCallPrompt(functionReq.getQueryText(),
                functionReq.getPluginConfigs());
        String response = chatLanguageModel.generate(functionCallPrompt);
        return functionCallParse(response);
    }

    public static FunctionResp functionCallParse(String llmOutput) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(llmOutput);
            String selectedTool = jsonNode.get("选择工具").asText();
            FunctionResp resp = new FunctionResp();
            resp.setToolSelection(selectedTool);
            return resp;
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }
}