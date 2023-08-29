package com.tencent.supersonic.chat.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.service.AgentService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping({"/api/chat/agent", "/openapi/chat/agent"})
public class AgentController {

    private AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public boolean createAgent(@RequestBody Agent agent,
                                HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        agentService.createAgent(agent, user);
        return true;
    }

    @PutMapping
    public boolean updateAgent(@RequestBody Agent agent,
                                HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        agentService.updateAgent(agent, user);
        return true;
    }

    @DeleteMapping("/{id}")
    public boolean deleteAgent(@PathVariable("id") Integer id) {
        agentService.deleteAgent(id);
        return true;
    }

    @RequestMapping("/getAgentList")
    public List<Agent> getAgentList() {
        return agentService.getAgents();
    }

}
