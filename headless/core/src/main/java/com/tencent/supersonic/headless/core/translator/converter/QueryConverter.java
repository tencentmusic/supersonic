package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * to supplement,translate the request Body
 */
public interface QueryConverter {

    boolean accept(QueryStatement queryStatement);

    void convert(QueryStatement queryStatement) throws Exception;

}
