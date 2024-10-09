package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.server.pojo.ChatModel;

import java.util.List;

public interface ChatModelService {
    List<ChatModel> getChatModels();

    ChatModel getChatModel(Integer id);

    ChatModel createChatModel(ChatModel chatModel, User user);

    ChatModel updateChatModel(ChatModel chatModel, User user);

    void deleteChatModel(Integer id);
}
