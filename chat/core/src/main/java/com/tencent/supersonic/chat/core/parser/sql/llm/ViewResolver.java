package com.tencent.supersonic.chat.core.parser.sql.llm;


import com.tencent.supersonic.chat.core.pojo.QueryContext;

import java.util.Set;

public interface ViewResolver {

    Long resolve(QueryContext queryContext, Set<Long> restrictiveModels);

}
