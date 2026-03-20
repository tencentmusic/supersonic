package com.tencent.supersonic.chat.api.service;

import com.tencent.supersonic.chat.api.pojo.ChatContext;

public interface ChatContextService {

    ChatContext getOrCreateContext(Integer chatId);

    void updateContext(ChatContext chatCtx);
}
