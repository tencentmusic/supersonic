package com.tencent.supersonic.chat.agent.tool;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonAgentTool extends AgentTool {

    protected List<Long> modelIds;

}