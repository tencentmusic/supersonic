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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatModelServiceImpl extends ServiceImpl<ChatModelMapper, ChatModelDO>
        implements ChatModelService {

    @Override
    public List<ChatModel> getChatModels(User user) {
        // 获取所有 ChatModel，并过滤仅返回用户有权限的 ChatModel
        return list().stream()
                .map(this::convert)
                .filter(chatModelResp -> hasPermission(chatModelResp, user))
                .collect(Collectors.toList());
    }

    /**
     * 检查给定的 ChatModel 是否有权限返回给用户
     *
     * @param chatModelResp 要检查的 ChatModel 对象
     * @param user          当前用户
     * @return 如果用户具有访问权限（在 viewers 或 admins 列表中），则返回 true；否则返回 false
     */
    private boolean hasPermission(ChatModel chatModelResp, User user) {
        // 检查用户是否为当前 ChatModel 的管理员或创建者，或者是否为超级管理员
        if (chatModelResp.getAdmins().contains(user.getName())
                || user.getName().equalsIgnoreCase(chatModelResp.getCreatedBy())
                || user.isSuperAdmin()) {
            return true;
        }
        // 检查用户是否为当前 ChatModel 的查看者
        return chatModelResp.getViewers().contains(user.getName());
    }

    @Override
    public List<ChatModel> getChatModels() {
        return list().stream().map(this::convert).collect(Collectors.toList());
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
        chatModelDO.setUpdatedAt(new Date());
        if (StringUtils.isBlank(chatModel.getAdmin())) {
            chatModelDO.setAdmin(user.getName());
        }
        save(chatModelDO);
        chatModel.setId(chatModelDO.getId());
        return chatModel;
    }

    @Override
    public ChatModel updateChatModel(ChatModel chatModel, User user) {
        ChatModelDO chatModelDO = convert(chatModel);
        chatModelDO.setUpdatedBy(user.getName());
        chatModelDO.setUpdatedAt(new Date());
        if (StringUtils.isBlank(chatModel.getAdmin())) {
            chatModel.setAdmin(user.getName());
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
        if (StringUtils.isNotBlank(chatModelDO.getAdmin())) {
            chatModel.setAdmins(Arrays.asList(chatModelDO.getAdmin().split(",")));
        }
        if (StringUtils.isNotBlank(chatModelDO.getViewer())) {
            chatModel.setViewers(Arrays.asList(chatModelDO.getViewer().split(",")));
        }
        chatModel.setConfig(JsonUtil.toObject(chatModelDO.getConfig(), ChatModelConfig.class));
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
