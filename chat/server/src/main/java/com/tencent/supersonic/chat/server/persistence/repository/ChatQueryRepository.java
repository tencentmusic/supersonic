package com.tencent.supersonic.chat.server.persistence.repository;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;

import java.util.List;

public interface ChatQueryRepository {

    PageInfo<QueryResp> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, Long chatId);

    QueryResp getChatQuery(Long queryId);

    ChatQueryDO getChatQueryDO(Long queryId);

    List<QueryResp> queryShowCase(PageQueryInfoReq pageQueryInfoCommend, int agentId);

    void updateChatParseInfo(List<ChatParseDO> chatParseDOS);

    ChatQueryDO getLastChatQuery(long chatId);

    int updateChatQuery(ChatQueryDO chatQueryDO);

    List<ChatParseDO> batchSaveParseInfo(ChatContext chatCtx, QueryReq queryReq,
                               ParseResp parseResult,
                               List<SemanticParseInfo> candidateParses);

    ChatParseDO getParseInfo(Long questionId, int parseId);

    List<ChatParseDO> getParseInfoList(List<Long> questionIds);

    Boolean deleteChatQuery(Long questionId);
}
