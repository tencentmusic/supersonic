package com.tencent.supersonic.common.service;


import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;

public interface ChatModelService {
    List<ChatModel> getChatModels();

    ChatModel getChatModel(Integer id);

    ChatModel createChatModel(ChatModel chatModel, User user);

    ChatModel updateChatModel(ChatModel chatModel, User user);

    void deleteChatModel(Integer id);
}
