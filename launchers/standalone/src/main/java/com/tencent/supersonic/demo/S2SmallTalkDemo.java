package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.ToolConfig;
import com.tencent.supersonic.chat.server.executor.PlainTextExecutor;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.chat.parser.llm.OnePassSCSqlGenStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(10)
public class S2SmallTalkDemo extends S2BaseDemo {

    public void doRun() {
        Agent agent = new Agent();
        agent.setName("闲聊助手");
        agent.setDescription("直接与大模型对话，验证连通性");
        agent.setStatus(1);
        agent.setEnableSearch(0);
        ToolConfig toolConfig = new ToolConfig();
        agent.setToolConfig(JSONObject.toJSONString(toolConfig));
        agent.setExamples(Lists.newArrayList("如何才能变帅", "如何才能赚更多钱", "如何才能世界和平"));

        // configure chat apps
        Map<String, ChatApp> chatAppConfig =
                Maps.newHashMap(ChatAppManager.getAllApps(AppModule.CHAT));
        chatAppConfig.values().forEach(app -> app.setChatModelId(demoChatModel.getId()));
        chatAppConfig.get(PlainTextExecutor.APP_KEY).setEnable(true);
        chatAppConfig.get(OnePassSCSqlGenStrategy.APP_KEY).setEnable(false);
        agent.setChatAppConfig(chatAppConfig);
        agent.setAdmins(Lists.newArrayList("jack"));
        agent.setViewers(Lists.newArrayList("alice", "tom"));
        agentService.createAgent(agent, defaultUser);
    }

    @Override
    protected boolean checkNeedToRun() {
        List<String> agentNames =
                agentService.getAgents().stream().map(Agent::getName).collect(Collectors.toList());
        return !agentNames.contains("闲聊助手");
    }

}
