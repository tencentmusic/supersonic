package com.tencent.supersonic.chat.domain.repository;

import com.tencent.supersonic.chat.domain.dataobject.ChatDO;
import com.tencent.supersonic.chat.domain.dataobject.QueryDO;
import java.util.List;

public interface ChatRepository {

    boolean createChat(ChatDO chatDO);

    List<ChatDO> getAll(String creator);

    Boolean updateChatName(Long chatId, String chatName, String lastTime, String creator);

    Boolean updateConversionIsTop(Long chatId, int isTop);

    boolean updateFeedback(QueryDO queryDO);

    Boolean deleteChat(Long chatId, String userName);
}
