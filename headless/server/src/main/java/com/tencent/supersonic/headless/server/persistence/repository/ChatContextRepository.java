package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.chat.ChatContext;

public interface ChatContextRepository {

    ChatContext getOrCreateContext(Integer chatId);

    void updateContext(ChatContext chatCtx);

}
