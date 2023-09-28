package com.tencent.supersonic.chat.queryresponder;

import com.tencent.supersonic.chat.api.pojo.request.SolvedQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.SolvedQueryRecallResp;
import java.util.List;

public interface QueryResponder {

    void saveSolvedQuery(SolvedQueryReq solvedQueryReq);

    List<SolvedQueryRecallResp> recallSolvedQuery(String queryText, Integer agentId);

}