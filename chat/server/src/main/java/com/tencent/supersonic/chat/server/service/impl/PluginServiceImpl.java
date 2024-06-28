package com.tencent.supersonic.chat.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.PluginQueryReq;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.server.plugin.event.PluginAddEvent;
import com.tencent.supersonic.chat.server.plugin.event.PluginDelEvent;
import com.tencent.supersonic.chat.server.plugin.event.PluginUpdateEvent;
import com.tencent.supersonic.chat.server.persistence.dataobject.PluginDO;
import com.tencent.supersonic.chat.server.persistence.repository.PluginRepository;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PluginServiceImpl implements PluginService {

    private PluginRepository pluginRepository;

    private ApplicationEventPublisher publisher;

    public PluginServiceImpl(PluginRepository pluginRepository,
            ApplicationEventPublisher publisher) {
        this.pluginRepository = pluginRepository;
        this.publisher = publisher;
    }

    @Override
    public synchronized void createPlugin(ChatPlugin plugin, User user) {
        PluginDO pluginDO = convert(plugin, user);
        pluginRepository.createPlugin(pluginDO);
        //compatible with H2 db
        List<ChatPlugin> plugins = getPluginList();
        publisher.publishEvent(new PluginAddEvent(this, plugins.get(plugins.size() - 1)));
    }

    @Override
    public void updatePlugin(ChatPlugin plugin, User user) {
        Long id = plugin.getId();
        PluginDO pluginDO = pluginRepository.getPlugin(id);
        ChatPlugin oldPlugin = convert(pluginDO);
        convert(plugin, pluginDO, user);
        pluginRepository.updatePlugin(pluginDO);
        publisher.publishEvent(new PluginUpdateEvent(this, oldPlugin, plugin));
    }

    @Override
    public void deletePlugin(Long id) {
        PluginDO pluginDO = pluginRepository.getPlugin(id);
        if (pluginDO != null) {
            pluginRepository.deletePlugin(id);
            publisher.publishEvent(new PluginDelEvent(this, convert(pluginDO)));
        }
    }

    @Override
    public List<ChatPlugin> getPluginList() {
        List<ChatPlugin> plugins = Lists.newArrayList();
        List<PluginDO> pluginDOS = pluginRepository.getPlugins();
        if (CollectionUtils.isEmpty(pluginDOS)) {
            return plugins;
        }
        return pluginDOS.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public List<ChatPlugin> fetchPluginDOs(String queryText, String type) {
        List<PluginDO> pluginDOS = pluginRepository.fetchPluginDOs(queryText, type);
        return convertList(pluginDOS);
    }

    @Override
    public List<ChatPlugin> query(PluginQueryReq pluginQueryReq) {
        QueryWrapper<PluginDO> queryWrapper = new QueryWrapper<>();

        if (StringUtils.isNotBlank(pluginQueryReq.getType())) {
            queryWrapper.lambda().eq(PluginDO::getType, pluginQueryReq.getType());
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getDataSet())) {
            queryWrapper.lambda().like(PluginDO::getDataSet, pluginQueryReq.getDataSet());
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getParseMode())) {
            queryWrapper.lambda().eq(PluginDO::getParseMode, pluginQueryReq.getParseMode());
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getName())) {
            queryWrapper.lambda().like(PluginDO::getName, pluginQueryReq.getName());
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getPattern())) {
            queryWrapper.lambda().like(PluginDO::getPattern, pluginQueryReq.getPattern());
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getCreatedBy())) {
            queryWrapper.lambda().eq(PluginDO::getCreatedBy, pluginQueryReq.getCreatedBy());
        }
        List<PluginDO> pluginDOS = pluginRepository.query(queryWrapper);
        if (StringUtils.isNotBlank(pluginQueryReq.getPattern())) {
            pluginDOS = pluginDOS.stream().filter(pluginDO ->
                            pluginDO.getPattern().contains(pluginQueryReq.getPattern())
                                    || (pluginDO.getName() != null
                                    && pluginDO.getName().contains(pluginQueryReq.getPattern())))
                    .collect(Collectors.toList());
        }
        return convertList(pluginDOS);
    }

    @Override
    public Optional<ChatPlugin> getPluginByName(String name) {
        log.info("name:{}", name);
        return getPluginList().stream()
                .filter(plugin -> {
                    PluginParseConfig functionCallConfig = getPluginParseConfig(plugin);
                    if (functionCallConfig == null) {
                        return false;
                    }
                    return functionCallConfig.getName().equalsIgnoreCase(name);
                })
                .findFirst();
    }

    private PluginParseConfig getPluginParseConfig(ChatPlugin plugin) {
        if (StringUtils.isBlank(plugin.getParseModeConfig())) {
            return null;
        }
        PluginParseConfig functionCallConfig = JsonUtil.toObject(
                plugin.getParseModeConfig(), PluginParseConfig.class);
        if (Objects.isNull(functionCallConfig) || StringUtils.isEmpty(functionCallConfig.getName())) {
            return null;
        }
        if (StringUtils.isBlank(functionCallConfig.getName())) {
            return null;
        }
        return functionCallConfig;
    }

    @Override
    public List<ChatPlugin> queryWithAuthCheck(PluginQueryReq pluginQueryReq, User user) {
        return authCheck(query(pluginQueryReq), user);
    }

    @Override
    public Map<String, ChatPlugin> getNameToPlugin() {
        List<ChatPlugin> pluginList = getPluginList();

        return pluginList.stream()
                .filter(plugin -> {
                    PluginParseConfig functionCallConfig = getPluginParseConfig(plugin);
                    if (functionCallConfig == null) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(a -> {
                    PluginParseConfig functionCallConfig = JsonUtil.toObject(
                            a.getParseModeConfig(), PluginParseConfig.class);
                    return functionCallConfig.getName();
                }, a -> a, (k1, k2) -> k1));
    }

    //todo
    private List<ChatPlugin> authCheck(List<ChatPlugin> plugins, User user) {
        return plugins;
    }

    public ChatPlugin convert(PluginDO pluginDO) {
        ChatPlugin plugin = new ChatPlugin();
        BeanUtils.copyProperties(pluginDO, plugin);
        if (StringUtils.isNotBlank(pluginDO.getDataSet())) {
            plugin.setDataSetList(Arrays.stream(pluginDO.getDataSet().split(","))
                    .map(Long::parseLong).collect(Collectors.toList()));
        }
        return plugin;
    }

    public PluginDO convert(ChatPlugin plugin, User user) {
        PluginDO pluginDO = new PluginDO();
        BeanUtils.copyProperties(plugin, pluginDO);
        pluginDO.setCreatedAt(new Date());
        pluginDO.setCreatedBy(user.getName());
        pluginDO.setUpdatedAt(new Date());
        pluginDO.setUpdatedBy(user.getName());
        pluginDO.setDataSet(StringUtils.join(plugin.getDataSetList(), ","));
        return pluginDO;
    }

    public PluginDO convert(ChatPlugin plugin, PluginDO pluginDO, User user) {
        BeanUtils.copyProperties(plugin, pluginDO);
        pluginDO.setUpdatedAt(new Date());
        pluginDO.setUpdatedBy(user.getName());
        pluginDO.setDataSet(StringUtils.join(plugin.getDataSetList(), ","));
        return pluginDO;
    }

    public List<ChatPlugin> convertList(List<PluginDO> pluginDOS) {
        if (!CollectionUtils.isEmpty(pluginDOS)) {
            return pluginDOS.stream().map(this::convert).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

}
