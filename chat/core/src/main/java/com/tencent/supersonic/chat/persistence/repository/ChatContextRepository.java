package com.tencent.supersonic.chat.persistence.repository;


import com.tencent.supersonic.chat.api.pojo.ChatContext;

public interface ChatContextRepository {

    ChatContext getOrCreateContext(int chatId);

    void updateContext(ChatContext chatCtx);

}
