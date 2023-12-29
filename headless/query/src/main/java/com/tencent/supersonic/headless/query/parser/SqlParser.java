package com.tencent.supersonic.headless.query.parser;

import com.tencent.supersonic.headless.common.query.enums.AggOption;
import com.tencent.supersonic.headless.model.domain.Catalog;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;

public interface SqlParser {

    QueryStatement explain(QueryStatement queryStatement, AggOption aggOption, Catalog catalog) throws Exception;
}
