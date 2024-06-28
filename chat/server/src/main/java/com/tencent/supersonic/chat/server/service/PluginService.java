package com.tencent.supersonic.chat.server.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.PluginQueryReq;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PluginService {

    void createPlugin(ChatPlugin plugin, User user);

    void updatePlugin(ChatPlugin plugin, User user);

    void deletePlugin(Long id);

    List<ChatPlugin> getPluginList();

    List<ChatPlugin> fetchPluginDOs(String queryText, String type);

    List<ChatPlugin> query(PluginQueryReq pluginQueryReq);

    Optional<ChatPlugin> getPluginByName(String name);

    List<ChatPlugin> queryWithAuthCheck(PluginQueryReq pluginQueryReq, User user);

    Map<String, ChatPlugin> getNameToPlugin();

}
