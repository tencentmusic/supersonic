package com.tencent.supersonic.chat.persistence.repository.impl;

import com.tencent.supersonic.chat.persistence.dataobject.AgentDO;
import com.tencent.supersonic.chat.persistence.dataobject.AgentDOExample;
import com.tencent.supersonic.chat.persistence.mapper.AgentDOMapper;
import com.tencent.supersonic.chat.persistence.repository.AgentRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class AgentRepositoryImpl implements AgentRepository {

    private AgentDOMapper agentDOMapper;

    public AgentRepositoryImpl(AgentDOMapper agentDOMapper) {
        this.agentDOMapper = agentDOMapper;
    }

    @Override
    public List<AgentDO> getAgents() {
        return agentDOMapper.selectByExample(new AgentDOExample());
    }

    @Override
    public void createAgent(AgentDO agentDO) {
        agentDOMapper.insert(agentDO);
    }

    @Override
    public void updateAgent(AgentDO agentDO) {
        agentDOMapper.updateByPrimaryKey(agentDO);
    }

    @Override
    public AgentDO getAgent(Integer id) {
        return agentDOMapper.selectByPrimaryKey(id);
    }

    @Override
    public void deleteAgent(Integer id) {
        agentDOMapper.deleteByPrimaryKey(id);
    }
}
