package com.tencent.supersonic.chat.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.api.pojo.request.PluginQueryReq;

import java.util.List;
import java.util.Optional;

public interface PluginService {

    void createPlugin(Plugin plugin, User user);

    void updatePlugin(Plugin plugin, User user);

    void deletePlugin(Long id);

    List<Plugin> getPluginList();

    List<Plugin> fetchPluginDOs(String queryText, String type);

    List<Plugin> query(PluginQueryReq pluginQueryReq);

    Optional<Plugin> getPluginByName(String name);

    List<Plugin> queryWithAuthCheck(PluginQueryReq pluginQueryReq, User user);
}
