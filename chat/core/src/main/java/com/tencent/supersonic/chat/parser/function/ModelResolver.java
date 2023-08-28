package com.tencent.supersonic.chat.parser.function;


import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import java.util.List;
import java.util.Set;

public interface ModelResolver {

    Long resolve(QueryContext queryContext, ChatContext chatCtx, Set<Long> restrictiveModels);

}