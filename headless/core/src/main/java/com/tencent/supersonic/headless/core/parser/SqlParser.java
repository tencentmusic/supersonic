package com.tencent.supersonic.headless.core.parser;

import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

public interface SqlParser {

    QueryStatement explain(QueryStatement queryStatement, AggOption aggOption) throws Exception;
}
