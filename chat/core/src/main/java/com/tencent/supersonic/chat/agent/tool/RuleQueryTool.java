package com.tencent.supersonic.chat.agent.tool;


import lombok.Data;

import java.util.List;

@Data
public class RuleQueryTool extends AgentTool {

    private List<String> queryModes;

}
