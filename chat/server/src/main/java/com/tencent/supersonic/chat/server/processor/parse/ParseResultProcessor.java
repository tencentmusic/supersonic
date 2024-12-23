package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.processor.ResultProcessor;

/** A ParseResultProcessor wraps things up before returning parsing results to the users. */
public interface ParseResultProcessor extends ResultProcessor {

    boolean accept(ParseContext parseContext);

    void process(ParseContext parseContext);
}
