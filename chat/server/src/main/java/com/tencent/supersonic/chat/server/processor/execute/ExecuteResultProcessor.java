package com.tencent.supersonic.chat.server.processor.execute;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.processor.ResultProcessor;

/** A ExecuteResultProcessor wraps things up before returning execution results to the users. */
public interface ExecuteResultProcessor extends ResultProcessor {

    void process(ExecuteContext executeContext, QueryResult queryResult);
}
