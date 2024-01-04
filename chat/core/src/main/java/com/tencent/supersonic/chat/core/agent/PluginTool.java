package com.tencent.supersonic.chat.core.agent;


import lombok.Data;

import java.util.List;

@Data
public class PluginTool extends AgentTool {

    private List<Long> plugins;

}