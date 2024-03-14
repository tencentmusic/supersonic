package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;

public interface ChatExecutor {

    QueryResult execute(ChatExecuteContext chatExecuteContext);

}
