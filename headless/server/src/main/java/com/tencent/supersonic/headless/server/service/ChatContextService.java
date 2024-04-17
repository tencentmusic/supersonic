package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.core.pojo.ChatContext;

public interface ChatContextService {

    /***
     * get the model from context
     * @param chatId
     * @return
     */
    Long getContextModel(Integer chatId);

    ChatContext getOrCreateContext(Integer chatId);

    void updateContext(ChatContext chatCtx);

}
