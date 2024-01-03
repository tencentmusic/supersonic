package com.tencent.supersonic.headless.core.parser;

import com.tencent.supersonic.headless.api.enums.AggOption;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.service.Catalog;

public interface SqlParser {

    QueryStatement explain(QueryStatement queryStatement, AggOption aggOption, Catalog catalog) throws Exception;
}
