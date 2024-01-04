package com.tencent.supersonic.chat.server.application.parser;

import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.core.parser.sql.rule.TimeRangeParser;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import org.junit.jupiter.api.Test;


class TimeRangeParserTest {

    @Test
    void parse() {
        TimeRangeParser timeRangeParser = new TimeRangeParser();

        QueryReq queryRequest = new QueryReq();
        ChatContext chatCtx = new ChatContext();

        queryRequest.setQueryText("supersonic最近30天访问次数");

        timeRangeParser.parse(QueryContext.builder().request(queryRequest).semanticSchema(null).build(), chatCtx);

    }
}
