package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.PluginDO;
import com.tencent.supersonic.chat.server.persistence.mapper.PluginDOMapper;
import com.tencent.supersonic.chat.server.persistence.repository.PluginRepository;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
@Slf4j
public class PluginRepositoryImpl implements PluginRepository {

    private PluginDOMapper pluginDOMapper;

    public PluginRepositoryImpl(PluginDOMapper pluginDOMapper) {
        this.pluginDOMapper = pluginDOMapper;
    }

    @Override
    public List<PluginDO> getPlugins() {
        return pluginDOMapper.selectList(new QueryWrapper<>());
    }

    @Override
    public List<PluginDO> fetchPluginDOs(String queryText, String type) {

        List<PluginDO> pluginDOList = new ArrayList<>();

        PluginRepository pluginRepository = ContextUtils.getBean(PluginRepository.class);
        List<PluginDO> pluginDOS = pluginRepository.getPlugins();

        for (PluginDO pluginDO : pluginDOS) {
            String pattern = pluginDO.getPattern();
            if (StringUtils.isNotEmpty(pattern)) {

                Pattern pluginPattern = Pattern.compile(pattern);
                Matcher pluginMatcher = pluginPattern.matcher(queryText);
                if (pluginMatcher.find()) {
                    log.info("pluginMatcher.find() is true, queryText:{}", queryText);
                    log.info("pluginDO:{}", pluginDO);
                    pluginDOList.add(pluginDO);
                }
            }
        }
        return pluginDOList;
    }

    @Override
    public void createPlugin(PluginDO pluginDO) {
        pluginDOMapper.insert(pluginDO);
    }

    @Override
    public void updatePlugin(PluginDO pluginDO) {
        pluginDOMapper.updateById(pluginDO);
    }

    @Override
    public PluginDO getPlugin(Long id) {
        return pluginDOMapper.selectById(id);
    }

    @Override
    public List<PluginDO> query(QueryWrapper<PluginDO> queryWrapper) {
        return pluginDOMapper.selectList(queryWrapper);
    }

    @Override
    public void deletePlugin(Long id) {
        pluginDOMapper.deleteById(id);
    }

}
