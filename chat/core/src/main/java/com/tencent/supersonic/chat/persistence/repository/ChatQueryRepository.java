package com.tencent.supersonic.chat.persistence.repository;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.api.pojo.response.QueryResponse;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;

public interface ChatQueryRepository {

    PageInfo<QueryResponse> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, long chatId);

    void createChatQuery(QueryResult queryResult, QueryRequest queryContext, ChatContext chatCtx);

    ChatQueryDO getLastChatQuery(long chatId);

    int updateChatQuery(ChatQueryDO chatQueryDO);
}
