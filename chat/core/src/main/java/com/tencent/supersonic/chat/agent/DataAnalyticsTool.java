package com.tencent.supersonic.chat.agent;

import com.tencent.supersonic.chat.parser.analytics.MetricOption;
import lombok.Data;

import java.util.List;


@Data
public class DataAnalyticsTool extends AgentTool {

    private Long modelId;

    private List<MetricOption> metricOptions;

}
