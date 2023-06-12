package com.tencent.supersonic.chat.infrastructure.mapper;

import com.tencent.supersonic.chat.domain.dataobject.ChatContextDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatContextMapper {

    ChatContextDO getContextByChatId(int chatId);

    int updateContext(ChatContextDO contextDO);

    int addContext(ChatContextDO contextDO);
}
