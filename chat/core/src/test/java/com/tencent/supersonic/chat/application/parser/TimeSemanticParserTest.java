package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.common.pojo.DateConf;
import org.junit.jupiter.api.Test;


class TimeSemanticParserTest {

    @Test
    void parse() {
        TimeSemanticParser timeSemanticParser = new TimeSemanticParser();

        QueryContextReq searchCtx = new QueryContextReq();
        ChatContext chatCtx = new ChatContext();
        SchemaMapInfo schemaMap = new SchemaMapInfo();
        searchCtx.setQueryText("supersonic最近30天访问次数");

        boolean parse = timeSemanticParser.parse(searchCtx, chatCtx);

        DateConf dateInfo = searchCtx.getParseInfo().getDateInfo();


    }
}