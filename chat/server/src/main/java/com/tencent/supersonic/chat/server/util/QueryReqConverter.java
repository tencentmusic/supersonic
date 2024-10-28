package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;

public class QueryReqConverter {

    public static QueryNLReq buildQueryNLReq(ParseContext parseContext) {
        QueryNLReq queryNLReq = new QueryNLReq();
        BeanMapper.mapper(parseContext.getRequest(), queryNLReq);
        queryNLReq.setText2SQLType(
                parseContext.enableLLM() ? Text2SQLType.RULE_AND_LLM : Text2SQLType.ONLY_RULE);
        queryNLReq.setDataSetIds(parseContext.getAgent().getDataSetIds());
        queryNLReq.setChatAppConfig(parseContext.getAgent().getChatAppConfig());
        queryNLReq.setSelectedParseInfo(parseContext.getRequest().getSelectedParse());

        return queryNLReq;
    }
}
