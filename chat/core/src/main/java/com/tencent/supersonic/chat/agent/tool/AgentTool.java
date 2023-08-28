package com.tencent.supersonic.chat.agent.tool;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentTool {

    private String name;

    private AgentToolType type;

}
