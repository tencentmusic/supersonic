package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.tencent.supersonic.chat.server.config.ChatConfig;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.server.config.ChatConfigFilterInternal;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatConfigDO;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatConfigMapper;
import com.tencent.supersonic.chat.server.util.ChatConfigHelper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatConfigRepository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
@Primary
public class ChatConfigRepositoryImpl implements ChatConfigRepository {

    private final ChatConfigHelper chatConfigHelper;
    private final ChatConfigMapper chatConfigMapper;

    public ChatConfigRepositoryImpl(ChatConfigHelper chatConfigHelper,
                                    ChatConfigMapper chatConfigMapper) {
        this.chatConfigHelper = chatConfigHelper;
        this.chatConfigMapper = chatConfigMapper;
    }

    @Override
    public Long createConfig(ChatConfig chaConfig) {
        ChatConfigDO chaConfigDO = chatConfigHelper.chatConfig2DO(chaConfig);
        chatConfigMapper.addConfig(chaConfigDO);
        return chaConfigDO.getId();
    }

    @Override
    public Long updateConfig(ChatConfig chaConfig) {
        ChatConfigDO chaConfigDO = chatConfigHelper.chatConfig2DO(chaConfig);

        return chatConfigMapper.editConfig(chaConfigDO);

    }

    @Override
    public List<ChatConfigResp> getChatConfig(ChatConfigFilter filter) {
        List<ChatConfigResp> chaConfigDescriptorList = new ArrayList<>();
        ChatConfigFilterInternal filterInternal = new ChatConfigFilterInternal();
        BeanUtils.copyProperties(filter, filterInternal);
        filterInternal.setStatus(filter.getStatus().getCode());
        List<ChatConfigDO> chaConfigDOList = chatConfigMapper.search(filterInternal);
        if (!CollectionUtils.isEmpty(chaConfigDOList)) {
            chaConfigDOList.stream().forEach(chaConfigDO ->
                    chaConfigDescriptorList.add(chatConfigHelper
                            .chatConfigDO2Descriptor(chaConfigDO.getModelId(), chaConfigDO)));
        }
        return chaConfigDescriptorList;
    }

    @Override
    public ChatConfigResp getConfigByModelId(Long modelId) {
        ChatConfigDO chaConfigPO = chatConfigMapper.fetchConfigByModelId(modelId);
        return chatConfigHelper.chatConfigDO2Descriptor(modelId, chaConfigPO);
    }

}
