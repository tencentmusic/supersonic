package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.headless.core.pojo.QueryContext;

import java.util.Set;

public interface DataSetResolver {

    Long resolve(QueryContext queryContext, Set<Long> restrictiveModels);

}
