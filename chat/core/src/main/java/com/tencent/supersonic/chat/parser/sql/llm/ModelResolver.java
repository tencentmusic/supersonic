package com.tencent.supersonic.chat.parser.sql.llm;


import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import java.util.Set;

public interface ModelResolver {

    String resolve(QueryContext queryContext, ChatContext chatCtx, Set<Long> restrictiveModels);

}
