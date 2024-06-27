package com.tencent.supersonic.chat.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.MultiTurnConfig;
import com.tencent.supersonic.chat.server.persistence.dataobject.AgentDO;
import com.tencent.supersonic.chat.server.persistence.mapper.AgentDOMapper;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.config.VisualConfig;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.config.LLMConfig;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl extends ServiceImpl<AgentDOMapper, AgentDO>
        implements AgentService {

    @Override
    public List<Agent> getAgents() {
        return getAgentDOList().stream()
                .map(this::convert).collect(Collectors.toList());
    }

    @Override
    public Integer createAgent(Agent agent, User user) {
        agent.createdBy(user.getName());
        AgentDO agentDO = convert(agent);
        save(agentDO);
        return agentDO.getId();
    }

    @Override
    public void updateAgent(Agent agent, User user) {
        agent.updatedBy(user.getName());
        updateById(convert(agent));
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

    private List<AgentDO> getAgentDOList() {
        return list();
    }

    private Agent convert(AgentDO agentDO) {
        if (agentDO == null) {
            return null;
        }
        Agent agent = new Agent();
        BeanUtils.copyProperties(agentDO, agent);
        agent.setAgentConfig(agentDO.getConfig());
        agent.setExamples(JsonUtil.toList(agentDO.getExamples(), String.class));
        agent.setLlmConfig(JsonUtil.toObject(agentDO.getLlmConfig(), LLMConfig.class));
        agent.setMultiTurnConfig(JsonUtil.toObject(agentDO.getMultiTurnConfig(), MultiTurnConfig.class));
        agent.setVisualConfig(JsonUtil.toObject(agentDO.getVisualConfig(), VisualConfig.class));
        return agent;
    }

    private AgentDO convert(Agent agent) {
        AgentDO agentDO = new AgentDO();
        BeanUtils.copyProperties(agent, agentDO);
        agentDO.setConfig(agent.getAgentConfig());
        agentDO.setExamples(JsonUtil.toString(agent.getExamples()));
        agentDO.setLlmConfig(JsonUtil.toString(agent.getLlmConfig()));
        agentDO.setMultiTurnConfig(JsonUtil.toString(agent.getMultiTurnConfig()));
        agentDO.setVisualConfig(JsonUtil.toString(agent.getVisualConfig()));
        if (agentDO.getStatus() == null) {
            agentDO.setStatus(1);
        }
        return agentDO;
    }

}
