package com.tencent.supersonic.chat.api.component;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.response.QueryResultResp;

/**
 * This class defines the contract for a semantic query that executes specific type of
 * query based on the results of semantic parsing.
 */
public interface SemanticQuery {

    String getQueryMode();

    QueryResultResp execute(User user);

    SemanticParseInfo getParseInfo();
}
