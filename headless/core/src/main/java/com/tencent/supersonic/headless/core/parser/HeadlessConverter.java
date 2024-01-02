package com.tencent.supersonic.headless.core.parser;

import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.service.Catalog;

public interface HeadlessConverter {

    boolean accept(QueryStatement queryStatement);

    void converter(Catalog catalog, QueryStatement queryStatement) throws Exception;

}
