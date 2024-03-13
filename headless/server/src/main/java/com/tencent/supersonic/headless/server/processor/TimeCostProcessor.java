package com.tencent.supersonic.headless.server.processor;


import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;

/**
 * TimeCostProcessor adds time cost of parsing.
 **/
@Slf4j
public class TimeCostProcessor implements ResultProcessor {

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        long parseStartTime = parseResp.getParseTimeCost().getParseStartTime();
        parseResp.getParseTimeCost().setParseTime(
                System.currentTimeMillis() - parseStartTime - parseResp.getParseTimeCost().getSqlTime());
    }

}
