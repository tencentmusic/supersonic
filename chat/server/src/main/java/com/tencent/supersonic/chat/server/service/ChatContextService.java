package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.server.pojo.ChatContext;

public interface ChatContextService {

    ChatContext getOrCreateContext(Integer chatId);

    void updateContext(ChatContext chatCtx);
}
