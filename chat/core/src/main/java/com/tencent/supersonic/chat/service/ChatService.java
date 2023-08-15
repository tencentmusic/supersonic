package com.tencent.supersonic.chat.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import java.util.List;

public interface ChatService {

    /***
     * get the model from context
     * @param chatId
     * @return
     */
    public Long getContextModel(Integer chatId);

    public ChatContext getOrCreateContext(int chatId);

    public void updateContext(ChatContext chatCtx);

    public void switchContext(ChatContext chatCtx);

    public Boolean addChat(User user, String chatName);

    public List<ChatDO> getAll(String userName);

    public boolean updateChatName(Long chatId, String chatName, String userName);

    public boolean updateFeedback(Integer id, Integer score, String feedback);

    public boolean updateChatIsTop(Long chatId, int isTop);

    Boolean deleteChat(Long chatId, String userName);

    PageInfo<QueryResp> queryInfo(PageQueryInfoReq pageQueryInfoCommend, long chatId);

    public void addQuery(QueryResult queryResult, ChatContext chatCtx);

    public ChatQueryDO getLastQuery(long chatId);

    public int updateQuery(ChatQueryDO chatQueryDO);
}
