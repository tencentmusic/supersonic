package com.tencent.supersonic.chat.server.processor.execute;

import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.processor.ResultProcessor;

/** A ExecuteResultProcessor wraps things up before returning execution results to the users. */
public interface ExecuteResultProcessor extends ResultProcessor {

    boolean accept(ExecuteContext executeContext);

    void process(ExecuteContext executeContext);
}
