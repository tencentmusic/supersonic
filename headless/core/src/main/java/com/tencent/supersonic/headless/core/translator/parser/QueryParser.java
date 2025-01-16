package com.tencent.supersonic.headless.core.translator.parser;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * A query parser generates physical SQL for the QueryStatement.
 */
public interface QueryParser {

    boolean accept(QueryStatement queryStatement);

    void parse(QueryStatement queryStatement) throws Exception;
}
