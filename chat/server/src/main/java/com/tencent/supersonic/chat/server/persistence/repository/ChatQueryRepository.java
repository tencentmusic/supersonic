package com.tencent.supersonic.chat.server.persistence.repository;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;

import java.util.List;

public interface ChatQueryRepository {

    PageInfo<QueryResp> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, Long chatId);

    QueryResp getChatQuery(Long queryId);

    ChatQueryDO getChatQueryDO(Long queryId);

    List<QueryResp> queryShowCase(PageQueryInfoReq pageQueryInfoCommend, int agentId);

    ChatQueryDO getLastChatQuery(long chatId);

    int updateChatQuery(ChatQueryDO chatQueryDO);

    Long createChatQuery(ChatParseReq chatParseReq);

    List<ChatParseDO> batchSaveParseInfo(ChatParseReq chatParseReq, ParseResp parseResult,
                                         List<SemanticParseInfo> candidateParses);

    ChatParseDO getParseInfo(Long questionId, int parseId);

    List<ChatParseDO> getParseInfoList(List<Long> questionIds);

    Boolean deleteChatQuery(Long questionId);

    List<ParseResp> getContextualParseInfo(Integer chatId);

}
