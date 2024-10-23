package com.tencent.supersonic.chat.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.VisualConfig;
import com.tencent.supersonic.chat.server.persistence.dataobject.AgentDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.persistence.mapper.AgentDOMapper;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentServiceImpl extends ServiceImpl<AgentDOMapper, AgentDO> implements AgentService {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private ChatQueryService chatQueryService;

    @Autowired
    private ChatModelService chatModelService;

    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    public List<Agent> getAgents() {
        return getAgentDOList().stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public Agent createAgent(Agent agent, User user) {
        agent.createdBy(user.getName());
        AgentDO agentDO = convert(agent);
        save(agentDO);
        agent.setId(agentDO.getId());
        executeAgentExamplesAsync(agent);
        return agent;
    }

    @Override
    public Agent updateAgent(Agent agent, User user) {
        agent.updatedBy(user.getName());
        updateById(convert(agent));
        executeAgentExamplesAsync(agent);
        return agent;
    }

    @Override
    public Agent getAgent(Integer id) {
        if (id == null) {
            return null;
        }
        return convert(getById(id));
    }

    @Override
    public void deleteAgent(Integer id) {
        removeById(id);
    }

    /**
     * the example in the agent will be executed by default, if the result is correct, it will be
     * put into memory as a reference for LLM
     *
     * @param agent
     */
    private void executeAgentExamplesAsync(Agent agent) {
        executorService.execute(() -> doExecuteAgentExamples(agent));
    }

    private synchronized void doExecuteAgentExamples(Agent agent) {
        if (!agent.containsDatasetTool() || !agent.enableMemoryReview()
                || CollectionUtils.isEmpty(agent.getExamples())) {
            return;
        }

        List<String> examples = agent.getExamples();
        ChatMemoryFilter chatMemoryFilter =
                ChatMemoryFilter.builder().agentId(agent.getId()).questions(examples).build();
        List<String> memoriesExisted = memoryService.getMemories(chatMemoryFilter).stream()
                .map(ChatMemoryDO::getQuestion).collect(Collectors.toList());
        for (String example : examples) {
            if (memoriesExisted.contains(example)) {
                continue;
            }
            try {
                chatQueryService
                        .parseAndExecute(ChatParseReq.builder().chatId(-1).agentId(agent.getId())
                                .queryText(example).user(User.getDefaultUser()).build());
            } catch (Exception e) {
                log.warn("agent:{} example execute failed:{}", agent.getName(), example);
            }
        }
    }

    private List<AgentDO> getAgentDOList() {
        return list();
    }

    private Agent convert(AgentDO agentDO) {
        if (agentDO == null) {
            return null;
        }
        Agent agent = new Agent();
        BeanUtils.copyProperties(agentDO, agent);
        agent.setToolConfig(agentDO.getToolConfig());
        agent.setExamples(JsonUtil.toList(agentDO.getExamples(), String.class));
        agent.setChatAppConfig(
                JsonUtil.toMap(agentDO.getChatModelConfig(), String.class, ChatApp.class));
        agent.setVisualConfig(JsonUtil.toObject(agentDO.getVisualConfig(), VisualConfig.class));
        agent.getChatAppConfig().values().forEach(c -> {
            ChatModel chatModel = chatModelService.getChatModel(c.getChatModelId());
            if (Objects.nonNull(chatModel)) {
                c.setChatModelConfig(chatModelService.getChatModel(c.getChatModelId()).getConfig());
            }
        });
        return agent;
    }

    private AgentDO convert(Agent agent) {
        AgentDO agentDO = new AgentDO();
        BeanUtils.copyProperties(agent, agentDO);
        agentDO.setToolConfig(agent.getToolConfig());
        agentDO.setExamples(JsonUtil.toString(agent.getExamples()));
        agentDO.setChatModelConfig(JsonUtil.toString(agent.getChatAppConfig()));
        agentDO.setVisualConfig(JsonUtil.toString(agent.getVisualConfig()));
        if (agentDO.getStatus() == null) {
            agentDO.setStatus(1);
        }
        return agentDO;
    }
}
