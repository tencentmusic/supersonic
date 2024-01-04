package com.tencent.supersonic.chat.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import java.util.List;
import java.util.Set;

public interface ChatService {

    /***
     * get the model from context
     * @param chatId
     * @return
     */
    Set<Long> getContextModel(Integer chatId);

    ChatContext getOrCreateContext(int chatId);

    void updateContext(ChatContext chatCtx);

    Boolean addChat(User user, String chatName, Integer agentId);

    List<ChatDO> getAll(String userName, Integer agentId);

    boolean updateChatName(Long chatId, String chatName, String userName);

    boolean updateFeedback(Integer id, Integer score, String feedback);

    boolean updateChatIsTop(Long chatId, int isTop);

    Boolean deleteChat(Long chatId, String userName);

    PageInfo<QueryResp> queryInfo(PageQueryInfoReq pageQueryInfoCommend, long chatId);

    QueryResp getChatQuery(Long queryId);

    ShowCaseResp queryShowCase(PageQueryInfoReq pageQueryInfoCommend, int agentId);

    List<ChatParseDO> batchAddParse(ChatContext chatCtx, QueryReq queryReq, ParseResp parseResult);

    ChatQueryDO getLastQuery(long chatId);

    int updateQuery(ChatQueryDO chatQueryDO);

    void updateQuery(Long questionId, int parseId, QueryResult queryResult, ChatContext chatCtx);

    ChatParseDO getParseInfo(Long questionId, int parseId);

    Boolean deleteChatQuery(Long questionId);
}
