package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentConfig;

import com.tencent.supersonic.chat.server.agent.MultiTurnConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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
        AgentConfig agentConfig = new AgentConfig();
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agent.setExamples(Lists.newArrayList("如何才能变帅",
                "如何才能赚更多钱", "如何才能世界和平"));
        MultiTurnConfig multiTurnConfig = new MultiTurnConfig();
        multiTurnConfig.setEnableMultiTurn(true);
        agent.setMultiTurnConfig(multiTurnConfig);

        agentService.createAgent(agent, User.getFakeUser());
    }

    @Override
    boolean checkNeedToRun() {
        return true;
    }
}
