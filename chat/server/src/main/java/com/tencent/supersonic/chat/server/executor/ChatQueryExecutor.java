package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;

public interface ChatQueryExecutor {

    QueryResult execute(ExecuteContext executeContext);
}
