package com.tencent.supersonic.chat.server.persistence.mapper;

import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.QueryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMapper {

    boolean createChat(ChatDO chatDO);

    List<ChatDO> getAll(String creator, Integer agentId);

    Boolean updateChatName(Long chatId, String chatName, String lastTime, String creator);

    Boolean updateLastQuestion(Long chatId, String lastQuestion, String lastTime);

    Boolean updateConversionIsTop(Long chatId, int isTop);

    boolean updateFeedback(QueryDO queryDO);

    Boolean deleteChat(Long chatId, String userName);
}
