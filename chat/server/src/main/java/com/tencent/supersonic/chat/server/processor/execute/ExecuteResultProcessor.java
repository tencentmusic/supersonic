package com.tencent.supersonic.chat.server.processor.execute;

import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.chat.server.processor.ResultProcessor;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;

/**
 * A ExecuteResultProcessor wraps things up before returning results to users in execute stage.
 */
public interface ExecuteResultProcessor extends ResultProcessor {

    void process(ChatExecuteContext chatExecuteContext, QueryResult queryResult);

}