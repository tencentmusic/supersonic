package com.tencent.supersonic.chat.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;

import java.util.List;

public interface ChatService {

    List<SearchResult> search(ChatParseReq chatParseReq);

    ParseResp performParsing(ChatParseReq chatParseReq);

    QueryResult performExecution(ChatExecuteReq chatExecuteReq) throws Exception;

    Object queryData(ChatQueryDataReq chatQueryDataReq, User user) throws Exception;

    SemanticParseInfo queryContext(Integer chatId);

    Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception;

    Boolean addChat(User user, String chatName, Integer agentId);

    List<ChatDO> getAll(String userName, Integer agentId);

    boolean updateChatName(Long chatId, String chatName, String userName);

    boolean updateFeedback(Integer id, Integer score, String feedback);

    boolean updateChatIsTop(Long chatId, int isTop);

    Boolean deleteChat(Long chatId, String userName);

    PageInfo<QueryResp> queryInfo(PageQueryInfoReq pageQueryInfoCommend, long chatId);

    QueryResp getChatQuery(Long queryId);

    ShowCaseResp queryShowCase(PageQueryInfoReq pageQueryInfoCommend, int agentId);

    List<ChatParseDO> batchAddParse(ChatParseReq chatParseReq, ParseResp parseResult);

    int updateQuery(ChatQueryDO chatQueryDO);

    void saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult);
}
