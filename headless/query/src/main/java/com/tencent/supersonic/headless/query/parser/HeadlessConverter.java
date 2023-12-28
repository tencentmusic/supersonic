package com.tencent.supersonic.headless.query.parser;

import com.tencent.supersonic.headless.model.domain.Catalog;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;

public interface HeadlessConverter {

    boolean accept(QueryStatement queryStatement);

    void converter(Catalog catalog, QueryStatement queryStatement)
            throws Exception;

}
