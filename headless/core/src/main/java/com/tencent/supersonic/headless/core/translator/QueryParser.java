package com.tencent.supersonic.headless.core.translator;

import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * A query parser generates physical SQL for the QueryStatement.
 */
public interface QueryParser {
    void parse(QueryStatement queryStatement, AggOption aggOption) throws Exception;
}
