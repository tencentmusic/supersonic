package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.MultiTurnConfig;
import com.tencent.supersonic.chat.server.agent.ToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(2)
public class SmallTalkDemo extends S2BaseDemo {

    public void doRun() {
        Agent agent = new Agent();
        agent.setName("来闲聊");
        agent.setDescription("直接与大模型对话，验证连通性");
        agent.setStatus(1);
        agent.setEnableSearch(0);
        ToolConfig toolConfig = new ToolConfig();
        agent.setToolConfig(JSONObject.toJSONString(toolConfig));
        agent.setExamples(Lists.newArrayList("如何才能变帅", "如何才能赚更多钱", "如何才能世界和平"));
        MultiTurnConfig multiTurnConfig = new MultiTurnConfig();
        multiTurnConfig.setEnableMultiTurn(true);
        agent.setMultiTurnConfig(multiTurnConfig);

        agentService.createAgent(agent, defaultUser);
    }

    @Override
    boolean checkNeedToRun() {
        List<String> agentNames =
                agentService.getAgents().stream().map(Agent::getName).collect(Collectors.toList());
        return !agentNames.contains("来闲聊");
    }
}
