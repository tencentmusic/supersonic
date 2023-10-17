package com.tencent.supersonic.chat.agent.tool;

import lombok.Data;

import java.util.List;

@Data
public class LLMParserTool extends CommonAgentTool {

    private List<String> exampleQuestions;

}
