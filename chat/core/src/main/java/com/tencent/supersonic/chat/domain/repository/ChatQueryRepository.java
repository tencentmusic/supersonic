package com.tencent.supersonic.chat.domain.repository;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.domain.pojo.chat.ChatQueryVO;
import com.tencent.supersonic.chat.domain.pojo.chat.PageQueryInfoReq;

public interface ChatQueryRepository {

    PageInfo<ChatQueryVO> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, long chatId);

    void createChatQuery(QueryResultResp queryResponse, QueryContextReq queryContext, ChatContext chatCtx);

}
