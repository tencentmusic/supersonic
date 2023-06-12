package com.tencent.supersonic.chat.infrastructure.repository;

import com.tencent.supersonic.chat.domain.dataobject.ChatConfigDO;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfig;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilter;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilterInternal;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import com.tencent.supersonic.chat.domain.repository.ChatConfigRepository;
import com.tencent.supersonic.chat.domain.utils.ChatConfigUtils;
import com.tencent.supersonic.chat.infrastructure.mapper.ChatConfigMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
public class ChatConfigRepositoryImpl implements ChatConfigRepository {

    private final ChatConfigUtils chatConfigUtils;
    private final ChatConfigMapper chatConfigMapper;

    public ChatConfigRepositoryImpl(ChatConfigUtils chatConfigUtils,
            ChatConfigMapper chatConfigMapper) {
        this.chatConfigUtils = chatConfigUtils;
        this.chatConfigMapper = chatConfigMapper;
    }

    @Override
    public Long createConfig(ChatConfig chaConfig) {
        ChatConfigDO chaConfigDO = chatConfigUtils.chatConfig2DO(chaConfig);

        return chatConfigMapper.addConfig(chaConfigDO);
    }

    @Override
    public Long updateConfig(ChatConfig chaConfig) {
        ChatConfigDO chaConfigDO = chatConfigUtils.chatConfig2DO(chaConfig);

        return chatConfigMapper.editConfig(chaConfigDO);

    }

    @Override
    public List<ChatConfigInfo> getChatConfig(ChatConfigFilter filter) {
        List<ChatConfigInfo> chaConfigDescriptorList = new ArrayList<>();
        ChatConfigFilterInternal filterInternal = new ChatConfigFilterInternal();
        BeanUtils.copyProperties(filter, filterInternal);
        filterInternal.setStatus(filter.getStatus().getCode());
        List<ChatConfigDO> chaConfigDOList = chatConfigMapper.search(filterInternal);
        if (!CollectionUtils.isEmpty(chaConfigDOList)) {
            chaConfigDOList.stream().forEach(chaConfigDO ->
                    chaConfigDescriptorList.add(chatConfigUtils.chatConfigDO2Descriptor(chaConfigDO)));
        }
        return chaConfigDescriptorList;
    }

    @Override
    public ChatConfigInfo getConfigByDomainId(Long domainId) {
        ChatConfigDO chaConfigPO = chatConfigMapper.fetchConfigByDomainId(domainId);
        if (Objects.isNull(chaConfigPO)) {
            return new ChatConfigInfo();
        }
        return chatConfigUtils.chatConfigDO2Descriptor(chaConfigPO);
    }

}