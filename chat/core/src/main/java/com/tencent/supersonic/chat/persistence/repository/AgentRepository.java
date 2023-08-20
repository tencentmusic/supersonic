package com.tencent.supersonic.chat.persistence.repository;

import com.tencent.supersonic.chat.persistence.dataobject.AgentDO;

import java.util.List;

public interface AgentRepository {

    List<AgentDO> getAgents();

    void createAgent(AgentDO agentDO);

    void updateAgent(AgentDO agentDO);

    AgentDO getAgent(Integer id);

    void deleteAgent(Integer id);
}
