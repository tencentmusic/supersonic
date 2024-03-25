package com.tencent.supersonic.headless.core.chat.query;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import org.apache.calcite.sql.parser.SqlParseException;

/**
 * A semantic query executes specific type of query based on the results of semantic parsing.
 */
public interface SemanticQuery {

    String getQueryMode();

    SemanticQueryReq buildSemanticQueryReq() throws SqlParseException;

    void initS2Sql(SemanticSchema semanticSchema, User user);

    SemanticParseInfo getParseInfo();

    void setParseInfo(SemanticParseInfo parseInfo);
}
