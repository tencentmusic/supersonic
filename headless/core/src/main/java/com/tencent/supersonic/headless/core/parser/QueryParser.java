package com.tencent.supersonic.headless.core.parser;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * A query parser takes semantic query request and generates SQL to be executed.
 */
public interface QueryParser {

    void parse(QueryStatement queryStatement) throws Exception;

}
