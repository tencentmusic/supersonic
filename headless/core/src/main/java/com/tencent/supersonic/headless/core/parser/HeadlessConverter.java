package com.tencent.supersonic.headless.core.parser;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

public interface HeadlessConverter {

    boolean accept(QueryStatement queryStatement);

    void convert(QueryStatement queryStatement) throws Exception;

}
