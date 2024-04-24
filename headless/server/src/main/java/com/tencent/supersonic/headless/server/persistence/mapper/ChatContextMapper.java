package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.server.persistence.dataobject.ChatContextDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatContextMapper {

    ChatContextDO getContextByChatId(Integer chatId);

    int updateContext(ChatContextDO contextDO);

    int addContext(ChatContextDO contextDO);
}
