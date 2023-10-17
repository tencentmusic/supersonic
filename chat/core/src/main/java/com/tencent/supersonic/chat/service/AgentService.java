package com.tencent.supersonic.chat.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.CommonAgentTool;
import java.util.List;
import java.util.Set;

public interface AgentService {

    List<Agent> getAgents();

    void createAgent(Agent agent, User user);

    void updateAgent(Agent agent, User user);

    Agent getAgent(Integer id);

    void deleteAgent(Integer id);

    List<CommonAgentTool> getParserTools(Integer agentId, AgentToolType agentToolType);

    Set<Long> getModelIds(Integer agentId, AgentToolType agentToolType);

    boolean containsAllModel(Set<Long> detectModelIds);
}
