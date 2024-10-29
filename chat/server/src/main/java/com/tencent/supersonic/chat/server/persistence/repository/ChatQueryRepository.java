package com.tencent.supersonic.chat.server.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

import java.util.List;

public interface ChatQueryRepository {

    PageInfo<QueryResp> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, Long chatId);

    QueryResp getChatQuery(Long queryId);

    List<QueryResp> getChatQueries(Integer chatId);

    ChatQueryDO getChatQueryDO(Long queryId);

    List<QueryResp> queryShowCase(PageQueryInfoReq pageQueryInfoCommend, int agentId);

    int updateChatQuery(ChatQueryDO chatQueryDO);

    void updateChatQuery(ChatQueryDO chatQueryDO, UpdateWrapper<ChatQueryDO> updateWrapper);

    Long createChatQuery(ChatParseReq chatParseReq);

    List<ChatParseDO> batchSaveParseInfo(ChatParseReq chatParseReq, ChatParseResp chatParseResp,
            List<SemanticParseInfo> candidateParses);

    ChatParseDO getParseInfo(Long questionId, int parseId);

    List<ChatParseDO> getParseInfoList(List<Long> questionIds);
}
