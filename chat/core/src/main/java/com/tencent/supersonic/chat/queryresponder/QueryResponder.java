package com.tencent.supersonic.chat.queryresponder;

import com.tencent.supersonic.chat.api.pojo.response.SolvedQueryRecallResp;
import java.util.List;

public interface QueryResponder {

    void saveSolvedQuery(String queryText, Long queryId, Integer parseId);

    List<SolvedQueryRecallResp> recallSolvedQuery(String queryText);

}