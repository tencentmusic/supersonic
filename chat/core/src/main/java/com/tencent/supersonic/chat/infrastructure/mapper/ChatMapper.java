package com.tencent.supersonic.chat.infrastructure.mapper;

import com.tencent.supersonic.chat.domain.dataobject.ChatDO;
import com.tencent.supersonic.chat.domain.dataobject.QueryDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMapper {

    boolean createChat(ChatDO chatDO);

    List<ChatDO> getAll(String creator);

    List<QueryDO> queryInfo(String userName, long chatId);

    Boolean updateChatName(Long chatId, String chatName, String lastTime, String creator);

    Boolean updateConversionIsTop(Long chatId, int isTop);

    boolean updateFeedback(QueryDO queryDO);

    Boolean deleteChat(Long chatId, String userName);

    Long createQuery(QueryDO queryDO);
}
