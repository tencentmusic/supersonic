package com.tencent.supersonic.chat.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.pojo.request.PluginQueryReq;
import com.tencent.supersonic.chat.persistence.dataobject.PluginDO;
import com.tencent.supersonic.chat.persistence.dataobject.PluginDOExample;
import com.tencent.supersonic.chat.persistence.repository.PluginRepository;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.plugin.event.PluginAddEvent;
import com.tencent.supersonic.chat.plugin.event.PluginDelEvent;
import com.tencent.supersonic.chat.plugin.event.PluginUpdateEvent;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    public synchronized void createPlugin(Plugin plugin, User user) {
        PluginDO pluginDO = convert(plugin, user);
        pluginRepository.createPlugin(pluginDO);
        //compatible with H2 db
        List<Plugin> plugins = getPluginList();
        publisher.publishEvent(new PluginAddEvent(this, plugins.get(plugins.size() - 1)));
    }

    @Override
    public void updatePlugin(Plugin plugin, User user) {
        Long id = plugin.getId();
        PluginDO pluginDO = pluginRepository.getPlugin(id);
        Plugin oldPlugin = convert(pluginDO);
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
    public List<Plugin> getPluginList() {
        List<Plugin> plugins = Lists.newArrayList();
        List<PluginDO> pluginDOS = pluginRepository.getPlugins();
        if (CollectionUtils.isEmpty(pluginDOS)) {
            return plugins;
        }
        return pluginDOS.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public List<Plugin> fetchPluginDOs(String queryText, String type) {
        List<PluginDO> pluginDOS = pluginRepository.fetchPluginDOs(queryText, type);
        return convertList(pluginDOS);
    }


    @Override
    public List<Plugin> query(PluginQueryReq pluginQueryReq) {
        PluginDOExample pluginDOExample = new PluginDOExample();
        pluginDOExample.createCriteria();
        if (StringUtils.isNotBlank(pluginQueryReq.getType())) {
            pluginDOExample.getOredCriteria().get(0).andTypeEqualTo(pluginQueryReq.getType());
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getModel())) {
            pluginDOExample.getOredCriteria().get(0).andModelLike('%' + pluginQueryReq.getModel() + '%');
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getParseMode())) {
            pluginDOExample.getOredCriteria().get(0).andParseModeEqualTo(pluginQueryReq.getParseMode());
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getName())) {
            pluginDOExample.getOredCriteria().get(0).andNameLike('%' + pluginQueryReq.getName() + '%');
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getPattern())) {
            pluginDOExample.getOredCriteria().get(0).andPatternLike('%' + pluginQueryReq.getPattern() + '%');
        }
        if (StringUtils.isNotBlank(pluginQueryReq.getCreatedBy())) {
            pluginDOExample.getOredCriteria().get(0).andCreatedByEqualTo(pluginQueryReq.getCreatedBy());
        }
        List<PluginDO> pluginDOS = pluginRepository.query(pluginDOExample);
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
    public Optional<Plugin> getPluginByName(String name) {
        log.info("name:{}", name);
        return getPluginList().stream()
                .filter(plugin -> {
                    if (StringUtils.isBlank(plugin.getParseModeConfig())) {
                        return false;
                    }
                    PluginParseConfig functionCallConfig = JsonUtil.toObject(
                            plugin.getParseModeConfig(), PluginParseConfig.class);
                    if (Objects.isNull(functionCallConfig) || StringUtils.isEmpty(functionCallConfig.getName())) {
                        return false;
                    }
                    if (StringUtils.isBlank(functionCallConfig.getName())) {
                        return false;
                    }
                    return functionCallConfig.getName().equalsIgnoreCase(name);
                })
                .findFirst();
    }

    @Override
    public List<Plugin> queryWithAuthCheck(PluginQueryReq pluginQueryReq, User user) {
        return authCheck(query(pluginQueryReq), user);
    }

    private List<Plugin> authCheck(List<Plugin> plugins, User user) {
        SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();
        List<Long> modelIdAuthorized = semanticInterpreter.getModelList(AuthType.ADMIN, null, user).stream()
                .map(ModelResp::getId).collect(Collectors.toList());
        plugins = plugins.stream().filter(plugin -> {
            if (CollectionUtils.isEmpty(plugin.getModelList()) || plugin.isContainsAllModel()) {
                return true;
            }
            for (Long modelId : plugin.getModelList()) {
                if (modelIdAuthorized.contains(modelId)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        return plugins;
    }

    public Plugin convert(PluginDO pluginDO) {
        Plugin plugin = new Plugin();
        BeanUtils.copyProperties(pluginDO, plugin);
        if (StringUtils.isNotBlank(pluginDO.getModel())) {
            plugin.setModelList(Arrays.stream(pluginDO.getModel().split(","))
                    .map(Long::parseLong).collect(Collectors.toList()));
        }
        return plugin;
    }

    public PluginDO convert(Plugin plugin, User user) {
        PluginDO pluginDO = new PluginDO();
        BeanUtils.copyProperties(plugin, pluginDO);
        pluginDO.setCreatedAt(new Date());
        pluginDO.setCreatedBy(user.getName());
        pluginDO.setUpdatedAt(new Date());
        pluginDO.setUpdatedBy(user.getName());
        pluginDO.setModel(StringUtils.join(plugin.getModelList(), ","));
        return pluginDO;
    }

    public PluginDO convert(Plugin plugin, PluginDO pluginDO, User user) {
        BeanUtils.copyProperties(plugin, pluginDO);
        pluginDO.setUpdatedAt(new Date());
        pluginDO.setUpdatedBy(user.getName());
        pluginDO.setModel(StringUtils.join(plugin.getModelList(), ","));
        return pluginDO;
    }

    public List<Plugin> convertList(List<PluginDO> pluginDOS) {
        if (!CollectionUtils.isEmpty(pluginDOS)) {
            return pluginDOS.stream().map(this::convert).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

}
