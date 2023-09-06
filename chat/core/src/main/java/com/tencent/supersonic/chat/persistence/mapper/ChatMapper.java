package com.tencent.supersonic.chat.persistence.mapper;

import com.tencent.supersonic.chat.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.persistence.dataobject.QueryDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

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
