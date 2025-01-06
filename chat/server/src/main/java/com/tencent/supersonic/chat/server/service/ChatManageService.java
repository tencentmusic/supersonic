package com.tencent.supersonic.chat.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;

import java.util.List;

public interface ChatManageService {
    Long addChat(User user, String chatName, Integer agentId);

    List<ChatDO> getAll(String userName, Integer agentId);

    boolean updateChatName(Long chatId, String chatName, String userName);

    boolean updateFeedback(Long id, Integer score, String feedback);

    boolean updateChatIsTop(Long chatId, int isTop);

    Boolean deleteChat(Long chatId, String userName);

    PageInfo<QueryResp> queryInfo(PageQueryInfoReq pageQueryInfoReq, long chatId);

    Long createChatQuery(ChatParseReq chatParseReq);

    QueryResp getChatQuery(Long queryId);

    List<QueryResp> getChatQueries(Integer chatId);

    ShowCaseResp queryShowCase(PageQueryInfoReq pageQueryInfoReq, int agentId);

    ChatQueryDO saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult);

    int updateQuery(ChatQueryDO chatQueryDO);

    void deleteQuery(Long queryId);

    void updateParseCostTime(ChatParseResp chatParseResp);

    List<ChatParseDO> batchAddParse(ChatParseReq chatParseReq, ChatParseResp chatParseResp);

    SemanticParseInfo getParseInfo(Long questionId, int parseId);
}
