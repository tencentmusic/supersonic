package com.tencent.supersonic.chat.server.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/chat/agent", "/openapi/chat/agent"})
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping
    public Agent createAgent(@RequestBody Agent agent, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return agentService.createAgent(agent, user);
    }

    @PutMapping
    public Agent updateAgent(@RequestBody Agent agent, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return agentService.updateAgent(agent, user);
    }

    @DeleteMapping("/{id}")
    public boolean deleteAgent(@PathVariable("id") Integer id) {
        agentService.deleteAgent(id);
        return true;
    }

    @RequestMapping("/getAgentList")
    public List<Agent> getAgentList(
            @RequestParam(value = "authType", required = false) AuthType authType,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return agentService.getAgents(user, authType);
    }

    @RequestMapping("/getToolTypes")
    public Map<AgentToolType, String> getToolTypes() {
        return AgentToolType.getToolTypes();
    }

}
