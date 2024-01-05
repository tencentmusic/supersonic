package com.tencent.supersonic.chat.core.parser.sql.llm;


import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import java.util.Set;

public interface ModelResolver {

    String resolve(QueryContext queryContext, ChatContext chatCtx, Set<Long> restrictiveModels);

}
