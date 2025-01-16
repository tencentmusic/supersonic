package com.tencent.supersonic.common.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.config.GeneralManageConfig;
import com.tencent.supersonic.common.persistence.dataobject.ChatModelDO;
import com.tencent.supersonic.common.persistence.mapper.ChatModelMapper;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatModelServiceImpl extends ServiceImpl<ChatModelMapper, ChatModelDO>
        implements ChatModelService {

    @Autowired
    private GeneralManageConfig generalManageConfig;

    @Override
    public List<ChatModel> getChatModels() {
        return list().stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public List<ChatModel> getChatModels(User user) {
        List<ChatModel> chatModelList =
                list().stream().map(this::convert).collect(Collectors.toList());
        setPermission(chatModelList, user);
        return chatModelList.stream().filter(ChatModel::isHasPermission)
                .collect(Collectors.toList());
    }

    private void setPermission(List<ChatModel> chatModelList, User user) {
        List<Integer> chatModelIds = generalManageConfig.getChatModelIds();
        boolean hasCommonModels = chatModelIds != null && !chatModelIds.isEmpty();
        chatModelList.forEach(chatModel -> {
            if (hasCommonModels && chatModelIds.contains(chatModel.getId())) {
                setCommonModelPermissions(chatModel);
            } else {
                setPermissionsForUser(chatModel, user);
            }
        });
    }

    private void setCommonModelPermissions(ChatModel chatModel) {
        chatModel.setHasPermission(true);
        chatModel.setHasUsePermission(true);
        chatModel.setHasEditPermission(false);
    }

    private void setPermissionsForUser(ChatModel chatModel, User user) {
        if (isAdminOrCreatorOrSuperAdmin(chatModel, user)) {
            chatModel.setHasPermission(true);
            chatModel.setHasEditPermission(true);
            chatModel.setHasUsePermission(true);
        } else if (isViewer(chatModel, user)) {
            chatModel.setHasPermission(true);
            chatModel.setHasUsePermission(true);
        }
    }

    private boolean isAdminOrCreatorOrSuperAdmin(ChatModel chatModel, User user) {
        return chatModel.getAdmins().contains(user.getName())
                || user.getName().equalsIgnoreCase(chatModel.getCreatedBy()) || user.isSuperAdmin();
    }

    private boolean isViewer(ChatModel chatModel, User user) {
        return chatModel.getViewers().contains(user.getName());
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
