package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.google.gson.Gson;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatContextDO;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatContextMapper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatContextRepository;
import com.tencent.supersonic.chat.server.pojo.ChatContext;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@Slf4j
public class ChatContextRepositoryImpl implements ChatContextRepository {

    private final ChatContextMapper chatContextMapper;

    public ChatContextRepositoryImpl(ChatContextMapper chatContextMapper) {
        this.chatContextMapper = chatContextMapper;
    }

    @Override
    public ChatContext getOrCreateContext(Integer chatId) {
        ChatContextDO context = chatContextMapper.getContextByChatId(chatId);
        if (context == null) {
            ChatContext chatContext = new ChatContext();
            chatContext.setChatId(chatId);
            return chatContext;
        }
        return cast(context);
    }

    @Override
    public void updateContext(ChatContext chatCtx) {
        chatContextMapper.insertOrUpdate(cast(chatCtx));
    }

    private ChatContext cast(ChatContextDO contextDO) {
        ChatContext chatContext = new ChatContext();
        chatContext.setChatId(contextDO.getChatId());
        chatContext.setUser(contextDO.getQueryUser());
        chatContext.setQueryText(contextDO.getQueryText());
        if (contextDO.getSemanticParse() != null && !contextDO.getSemanticParse().isEmpty()) {
            SemanticParseInfo semanticParseInfo =
                    JsonUtil.toObject(contextDO.getSemanticParse(), SemanticParseInfo.class);
            chatContext.setParseInfo(semanticParseInfo);
        }
        return chatContext;
    }

    private ChatContextDO cast(ChatContext chatContext) {
        ChatContextDO chatContextDO = new ChatContextDO();
        chatContextDO.setChatId(chatContext.getChatId());
        chatContextDO.setQueryText(chatContext.getQueryText());
        chatContextDO.setQueryUser(chatContext.getUser());
        if (chatContext.getParseInfo() != null) {
            Gson g = new Gson();
            chatContextDO.setSemanticParse(g.toJson(chatContext.getParseInfo()));
        }
        return chatContextDO;
    }
}
