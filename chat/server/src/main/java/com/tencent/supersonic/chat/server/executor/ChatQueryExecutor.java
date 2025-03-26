package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import dev.langchain4j.service.TokenStream;

public interface ChatQueryExecutor {

    boolean accept(ExecuteContext executeContext);

    QueryResult execute(ExecuteContext executeContext);

    TokenStream streamExecute(ExecuteContext executeContext);
}
