package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import lombok.extern.slf4j.Slf4j;

/**
 * TimeCostProcessor adds time cost of parsing.
 **/
@Slf4j
public class TimeCostCalcProcessor implements ParseResultProcessor {

    @Override
    public boolean accept(ParseContext parseContext) {
        return true;
    }

    @Override
    public void process(ParseContext parseContext) {
        ChatParseResp parseResp = parseContext.getResponse();
        long parseStartTime = parseResp.getParseTimeCost().getParseStartTime();
        parseResp.getParseTimeCost().setParseTime(System.currentTimeMillis() - parseStartTime
                - parseResp.getParseTimeCost().getSqlTime());
    }
}
