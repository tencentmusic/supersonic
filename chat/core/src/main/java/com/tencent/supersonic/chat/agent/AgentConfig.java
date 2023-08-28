package com.tencent.supersonic.chat.agent;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.agent.tool.AgentTool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {

    List<AgentTool> tools = Lists.newArrayList();

}
