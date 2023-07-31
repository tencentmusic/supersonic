package com.tencent.supersonic.chat.persistence.repository;

import com.tencent.supersonic.chat.persistence.dataobject.PluginDO;
import com.tencent.supersonic.chat.persistence.dataobject.PluginDOExample;

import java.util.List;

public interface PluginRepository {
    List<PluginDO> getPlugins();

    List<PluginDO> fetchPluginDOs(String queryText, String type);

    void createPlugin(PluginDO pluginDO);

    void updatePlugin(PluginDO pluginDO);

    PluginDO getPlugin(Long id);

    List<PluginDO> query(PluginDOExample pluginDOExample);

    void deletePlugin(Long id);
}
