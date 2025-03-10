package com.tencent.supersonic.common.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.persistence.dataobject.ChatModelDO;
import com.tencent.supersonic.common.persistence.mapper.ChatModelMapper;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatModelServiceImpl extends ServiceImpl<ChatModelMapper, ChatModelDO>
        implements ChatModelService {
    @Override
    public List<ChatModel> getChatModels(User user) {
        return list().stream().map(this::convert).filter(chatModel -> {
            if (chatModel.isPublic() || user.isSuperAdmin()
                    || chatModel.getCreatedBy().equals(user.getName())
                    || chatModel.getViewers().contains(user.getName())) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
    }

    @Override
    public ChatModel getChatModel(Integer id) {
        if (id == null) {
            return null;
        }
        return convert(getById(id));
    }

    @Override
    public ChatModel createChatModel(ChatModel chatModel, User user) {
        ChatModelDO chatModelDO = convert(chatModel);
        chatModelDO.setCreatedBy(user.getName());
        chatModelDO.setCreatedAt(new Date());
        chatModelDO.setUpdatedBy(user.getName());
        chatModelDO.setUpdatedAt(chatModelDO.getCreatedAt());
        chatModelDO.setIsOpen(chatModel.getIsOpen());
        if (StringUtils.isBlank(chatModel.getAdmin())) {
            chatModelDO.setAdmin(user.getName());
        }
        if (!chatModel.getViewers().isEmpty()) {
            chatModelDO.setViewer(JsonUtil.toString(chatModel.getViewers()));
        }
        save(chatModelDO);
        return chatModel;
    }

    @Override
    public ChatModel updateChatModel(ChatModel chatModel, User user) {
        ChatModelDO chatModelDO = convert(chatModel);
        chatModelDO.setUpdatedBy(user.getName());
        chatModelDO.setUpdatedAt(new Date());
        chatModelDO.setIsOpen(chatModel.getIsOpen());
        if (StringUtils.isBlank(chatModel.getAdmin())) {
            chatModel.setAdmin(user.getName());
        }
        if (!chatModel.getViewers().isEmpty()) {
            chatModelDO.setViewer(JsonUtil.toString(chatModel.getViewers()));
        }
        updateById(chatModelDO);
        return chatModel;
    }

    @Override
    public void deleteChatModel(Integer id) {
        removeById(id);
    }

    private ChatModel convert(ChatModelDO chatModelDO) {
        if (chatModelDO == null) {
            return null;
        }
        ChatModel chatModel = new ChatModel();
        BeanUtils.copyProperties(chatModelDO, chatModel);
        chatModel.setConfig(JsonUtil.toObject(chatModelDO.getConfig(), ChatModelConfig.class));
        chatModel.setViewers(JsonUtil.toList(chatModelDO.getViewer(), String.class));
        return chatModel;
    }

    private ChatModelDO convert(ChatModel chatModel) {
        if (chatModel == null) {
            return null;
        }
        ChatModelDO chatModelDO = new ChatModelDO();
        BeanUtils.copyProperties(chatModel, chatModelDO);
        chatModelDO.setConfig(JsonUtil.toString(chatModel.getConfig()));
        return chatModelDO;
    }
}
