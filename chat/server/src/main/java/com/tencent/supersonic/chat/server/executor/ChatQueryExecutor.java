package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;

public interface ChatQueryExecutor {

    QueryResult execute(ExecuteContext executeContext);

}
