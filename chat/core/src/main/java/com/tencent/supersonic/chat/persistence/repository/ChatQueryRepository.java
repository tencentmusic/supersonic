package com.tencent.supersonic.chat.persistence.repository;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;

import java.util.List;

public interface ChatQueryRepository {

    PageInfo<QueryResp> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, Long chatId);

    List<QueryResp> queryShowCase(PageQueryInfoReq pageQueryInfoCommend, int agentId);

    void createChatQuery(QueryResult queryResult, ChatContext chatCtx);

    ChatQueryDO getLastChatQuery(long chatId);

    int updateChatQuery(ChatQueryDO chatQueryDO);

    Long createChatParse(ParseResp parseResult, ChatContext chatCtx, QueryReq queryReq);

    Boolean batchSaveParseInfo(ChatContext chatCtx, QueryReq queryReq,
                               ParseResp parseResult,
                               List<SemanticParseInfo> candidateParses,
                               List<SemanticParseInfo> selectedParses);

    public ChatParseDO getParseInfo(Long questionId, String userName, int parseId);

    Boolean deleteChatQuery(Long questionId);
}
