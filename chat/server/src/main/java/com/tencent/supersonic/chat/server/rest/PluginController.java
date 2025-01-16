package com.tencent.supersonic.chat.server.rest;

import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.PluginQueryReq;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.pojo.User;
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

@RestController
@RequestMapping("/api/chat/plugin")
public class PluginController {

    @Autowired
    protected PluginService pluginService;

    @PostMapping
    public boolean createPlugin(@RequestBody ChatPlugin plugin,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        pluginService.createPlugin(plugin, user);
        return true;
    }

    @PutMapping
    public boolean updatePlugin(@RequestBody ChatPlugin plugin,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        pluginService.updatePlugin(plugin, user);
        return true;
    }

    @DeleteMapping("/{id}")
    public boolean deletePlugin(@PathVariable("id") Long id) {
        pluginService.deletePlugin(id);
        return true;
    }

    @RequestMapping("/getPluginList")
    public List<ChatPlugin> getPluginList() {
        return pluginService.getPluginList();
    }

    @PostMapping("/query")
    List<ChatPlugin> query(@RequestBody PluginQueryReq pluginQueryReq,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return pluginService.queryWithAuthCheck(pluginQueryReq, user);
    }

    @AuthenticationIgnore
    @PostMapping("/pluginDemo")
    public String pluginDemo(@RequestParam("queryText") String queryText,
            @RequestBody Object object) {
        return String.format("已收到您的问题:%s, 但这只是一个demo~", queryText);
    }
}
