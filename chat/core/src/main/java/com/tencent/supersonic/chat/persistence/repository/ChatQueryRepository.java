package com.tencent.supersonic.chat.persistence.repository;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;

public interface ChatQueryRepository {

    PageInfo<QueryResp> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, long chatId);

    void createChatQuery(QueryResult queryResult, ChatContext chatCtx);

    ChatQueryDO getLastChatQuery(long chatId);

    int updateChatQuery(ChatQueryDO chatQueryDO);
}
