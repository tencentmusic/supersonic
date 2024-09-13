package com.tencent.supersonic.chat.server.persistence.repository;

import com.tencent.supersonic.chat.server.pojo.ChatContext;

public interface ChatContextRepository {

    ChatContext getOrCreateContext(Integer chatId);

    void updateContext(ChatContext chatCtx);
}
