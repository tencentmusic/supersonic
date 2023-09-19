package com.tencent.supersonic.chat.api.component;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import org.apache.calcite.sql.parser.SqlParseException;

/**
 * A semantic query executes specific type of query based on the results of semantic parsing.
 */
public interface SemanticQuery {

    String getQueryMode();

    QueryResult execute(User user) throws SqlParseException;

    ExplainResp explain(User user);

    SemanticParseInfo getParseInfo();

    void setParseInfo(SemanticParseInfo parseInfo);
}
