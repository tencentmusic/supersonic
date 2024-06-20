package com.tencent.supersonic.chat.server.persistence.repository;

import com.tencent.supersonic.headless.chat.ChatContext;

public interface ChatContextRepository {

    ChatContext getOrCreateContext(int chatId);

    void updateContext(ChatContext chatCtx);

}
