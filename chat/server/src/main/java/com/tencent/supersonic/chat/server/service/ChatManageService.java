package com.tencent.supersonic.chat.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;

import java.util.List;

public interface ChatManageService {
    Long addChat(User user, String chatName, Integer agentId);

    List<ChatDO> getAll(String userName, Integer agentId);

    boolean updateChatName(Long chatId, String chatName, String userName);

    boolean updateFeedback(Integer id, Integer score, String feedback);

    boolean updateChatIsTop(Long chatId, int isTop);

    Boolean deleteChat(Long chatId, String userName);

    PageInfo<QueryResp> queryInfo(PageQueryInfoReq pageQueryInfoReq, long chatId);

    void createChatQuery(ChatParseReq chatParseReq, ParseResp parseResp);

    QueryResp getChatQuery(Long queryId);

    ShowCaseResp queryShowCase(PageQueryInfoReq pageQueryInfoReq, int agentId);

    ChatQueryDO saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult);

    int updateQuery(ChatQueryDO chatQueryDO);

    void updateParseCostTime(ParseResp parseResp);

    List<ChatParseDO> batchAddParse(ChatParseReq chatParseReq, ParseResp parseResult);

    SemanticParseInfo getParseInfo(Long questionId, int parseId);
}
