package com.tencent.supersonic.chat.server.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.PluginQueryReq;
import com.tencent.supersonic.chat.server.plugin.Plugin;
import com.tencent.supersonic.chat.server.service.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/chat/plugin")
public class PluginController {

    @Autowired
    protected PluginService pluginService;

    @PostMapping
    public boolean createPlugin(@RequestBody Plugin plugin,
                      HttpServletRequest httpServletRequest,
                      HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        pluginService.createPlugin(plugin, user);
        return true;
    }

    @PutMapping
    public boolean updatePlugin(@RequestBody Plugin plugin,
                                HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse) {
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
    public List<Plugin> getPluginList() {
        return pluginService.getPluginList();
    }

    @PostMapping("/query")
    List<Plugin> query(@RequestBody PluginQueryReq pluginQueryReq,
                       HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return pluginService.queryWithAuthCheck(pluginQueryReq, user);
    }

}
