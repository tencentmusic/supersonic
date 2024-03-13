package com.tencent.supersonic.chat.server.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentTool {

    private String id;

    private String name;

    private AgentToolType type;
}
