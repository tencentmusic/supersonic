package com.tencent.supersonic.chat.domain.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.domain.dataobject.ChatDO;
import com.tencent.supersonic.chat.domain.pojo.chat.ChatQueryVO;
import com.tencent.supersonic.chat.domain.pojo.chat.PageQueryInfoReq;
import java.util.List;

public interface ChatService {

    /***
     * get the domain from context
     * @param chatId
     * @return
     */
    public Long getContextDomain(Integer chatId);

    public ChatContext getOrCreateContext(int chatId);

    public void updateContext(ChatContext chatCtx);

    public void switchContext(ChatContext chatCtx);

    public Boolean addChat(User user, String chatName);

    public List<ChatDO> getAll(String userName);

    public boolean updateChatName(Long chatId, String chatName, String userName);

    public boolean updateFeedback(Integer id, Integer score, String feedback);

    public boolean updateChatIsTop(Long chatId, int isTop);

    Boolean deleteChat(Long chatId, String userName);

    PageInfo<ChatQueryVO> queryInfo(PageQueryInfoReq pageQueryInfoCommend, long chatId);

    public void addQuery(QueryResultResp queryResponse, QueryContextReq queryContext, ChatContext chatCtx);
}
