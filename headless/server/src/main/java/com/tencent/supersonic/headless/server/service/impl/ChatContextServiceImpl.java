package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.server.persistence.repository.ChatContextRepository;
import com.tencent.supersonic.headless.server.service.ChatContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Slf4j
@Service
public class ChatContextServiceImpl implements ChatContextService {

    private ChatContextRepository chatContextRepository;

    public ChatContextServiceImpl(ChatContextRepository chatContextRepository) {
        this.chatContextRepository = chatContextRepository;
    }

    @Override
    public Long getContextModel(Integer chatId) {
        if (Objects.isNull(chatId)) {
            return null;
        }
        ChatContext chatContext = getOrCreateContext(chatId);
        if (Objects.isNull(chatContext)) {
            return null;
        }
        SemanticParseInfo originalSemanticParse = chatContext.getParseInfo();
        if (Objects.nonNull(originalSemanticParse) && Objects.nonNull(originalSemanticParse.getDataSetId())) {
            return originalSemanticParse.getDataSetId();
        }
        return null;
    }

    @Override
    public ChatContext getOrCreateContext(Integer chatId) {
        return chatContextRepository.getOrCreateContext(chatId);
    }

    @Override
    public void updateContext(ChatContext chatCtx) {
        log.debug("save ChatContext {}", chatCtx);
        chatContextRepository.updateContext(chatCtx);
    }

}
