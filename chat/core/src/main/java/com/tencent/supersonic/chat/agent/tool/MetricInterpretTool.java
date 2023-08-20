package com.tencent.supersonic.chat.agent.tool;

import com.tencent.supersonic.chat.parser.llm.interpret.MetricOption;
import lombok.Data;

import java.util.List;


@Data
public class MetricInterpretTool extends AgentTool {

    private Long modelId;

    private List<MetricOption> metricOptions;

}
