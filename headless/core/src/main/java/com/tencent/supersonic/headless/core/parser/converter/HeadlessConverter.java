package com.tencent.supersonic.headless.core.parser.converter;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * to supplement,translate the request Body
 */
public interface HeadlessConverter {

    boolean accept(QueryStatement queryStatement);

    void convert(QueryStatement queryStatement) throws Exception;

}
