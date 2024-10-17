package com.tencent.supersonic.chat.server.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetTool extends AgentTool {

    private List<Long> dataSetIds;
    private List<String> exampleQuestions;
}
