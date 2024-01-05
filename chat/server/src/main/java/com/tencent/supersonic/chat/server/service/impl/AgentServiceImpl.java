package com.tencent.supersonic.chat.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.core.agent.Agent;
import com.tencent.supersonic.chat.server.persistence.dataobject.AgentDO;
import com.tencent.supersonic.chat.server.persistence.repository.AgentRepository;
import com.tencent.supersonic.chat.server.service.AgentService;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class AgentServiceImpl implements AgentService {

    private AgentRepository agentRepository;

    public AgentServiceImpl(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    public List<Agent> getAgents() {
        return getAgentDOList().stream()
                .map(this::convert).collect(Collectors.toList());
    }

    @Override
    public void createAgent(Agent agent, User user) {
        agentRepository.createAgent(convert(agent, user));
    }

    @Override
    public void updateAgent(Agent agent, User user) {
        agentRepository.updateAgent(convert(agent, user));
    }

    @Override
    public Agent getAgent(Integer id) {
        if (id == null) {
            return null;
        }
        return convert(agentRepository.getAgent(id));
    }

    @Override
    public void deleteAgent(Integer id) {
        agentRepository.deleteAgent(id);
    }

    private List<AgentDO> getAgentDOList() {
        return agentRepository.getAgents();
    }

    private Agent convert(AgentDO agentDO) {
        if (agentDO == null) {
            return null;
        }
        Agent agent = new Agent();
        BeanUtils.copyProperties(agentDO, agent);
        agent.setAgentConfig(agentDO.getConfig());
        agent.setExamples(JSONObject.parseArray(agentDO.getExamples(), String.class));
        return agent;
    }

    private AgentDO convert(Agent agent, User user) {
        AgentDO agentDO = new AgentDO();
        BeanUtils.copyProperties(agent, agentDO);
        agentDO.setConfig(agent.getAgentConfig());
        agentDO.setExamples(JSONObject.toJSONString(agent.getExamples()));
        agentDO.setCreatedAt(new Date());
        agentDO.setCreatedBy(user.getName());
        agentDO.setUpdatedAt(new Date());
        agentDO.setUpdatedBy(user.getName());
        if (agentDO.getStatus() == null) {
            agentDO.setStatus(1);
        }
        return agentDO;
    }

}
