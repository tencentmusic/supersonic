package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.parser.sql.rule.TimeRangeParser;
import org.junit.jupiter.api.Test;


class TimeRangeParserTest {

    @Test
    void parse() {
        TimeRangeParser timeRangeParser = new TimeRangeParser();

        QueryReq queryRequest = new QueryReq();
        ChatContext chatCtx = new ChatContext();

        queryRequest.setQueryText("supersonic最近30天访问次数");
        timeRangeParser.parse(new QueryContext(queryRequest), chatCtx);

    }
}
