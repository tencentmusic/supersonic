package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * A query converter performs preprocessing work to the QueryStatement before parsing.
 */
public interface QueryConverter {

    boolean accept(QueryStatement queryStatement);

    void convert(QueryStatement queryStatement) throws Exception;
}
