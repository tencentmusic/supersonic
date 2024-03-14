package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.server.service.ChatQueryService;

public class Text2SqlParser implements ChatParser {

    @Override
    public void parse(ChatParseContext chatParseContext, ParseResp parseResp) {
        QueryReq queryReq = QueryReqConverter.buildText2SqlQueryReq(chatParseContext);
        ChatQueryService chatQueryService = ContextUtils.getBean(ChatQueryService.class);
        ParseResp text2SqlParseResp = chatQueryService.performParsing(queryReq);
        if (!ParseResp.ParseState.FAILED.equals(text2SqlParseResp.getState())) {
            parseResp.getSelectedParses().addAll(text2SqlParseResp.getSelectedParses());
        }
    }

}
