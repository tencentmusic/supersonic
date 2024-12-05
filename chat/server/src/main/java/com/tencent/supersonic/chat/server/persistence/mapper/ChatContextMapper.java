package com.tencent.supersonic.chat.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatContextDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatContextMapper extends BaseMapper<ChatContextDO> {

    ChatContextDO getContextByChatId(Integer chatId);
}
