package com.tencent.supersonic.chat.processor;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeCostProcessor implements ResponseProcessor {


    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        long parseStartTime = parseResp.getParseTimeCost().getParseStartTime();
        parseResp.getParseTimeCost().setParseTime(
                System.currentTimeMillis() - parseStartTime - parseResp.getParseTimeCost().getSqlTime());
    }


}
