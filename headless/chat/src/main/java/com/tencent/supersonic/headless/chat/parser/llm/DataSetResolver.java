package com.tencent.supersonic.headless.chat.parser.llm;


import com.tencent.supersonic.headless.chat.QueryContext;

import java.util.Set;

public interface DataSetResolver {

    Long resolve(QueryContext queryContext, Set<Long> restrictiveModels);

}
