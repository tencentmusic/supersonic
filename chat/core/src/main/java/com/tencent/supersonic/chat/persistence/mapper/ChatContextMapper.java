package com.tencent.supersonic.chat.persistence.mapper;

import com.tencent.supersonic.chat.persistence.dataobject.ChatContextDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatContextMapper {

    ChatContextDO getContextByChatId(int chatId);

    int updateContext(ChatContextDO contextDO);

    int addContext(ChatContextDO contextDO);
}
