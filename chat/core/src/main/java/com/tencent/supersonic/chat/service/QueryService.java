package com.tencent.supersonic.chat.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.request.QueryDataRequest;
import org.apache.calcite.sql.parser.SqlParseException;

/***
 * QueryService for query and search
 */
public interface QueryService {

    QueryResult executeQuery(QueryRequest queryCtx) throws Exception;

    SemanticParseInfo queryContext(QueryRequest queryCtx);

    QueryResult executeDirectQuery(QueryDataRequest queryData, User user) throws SqlParseException;
}
