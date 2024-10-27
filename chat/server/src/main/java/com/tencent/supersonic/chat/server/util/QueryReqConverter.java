package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;

public class QueryReqConverter {

    public static QueryNLReq buildQueryNLReq(ParseContext parseContext) {
        if (parseContext.getAgent() == null) {
            return null;
        }

        QueryNLReq queryNLReq = new QueryNLReq();
        BeanMapper.mapper(parseContext.getRequest(), queryNLReq);
        queryNLReq.setText2SQLType(parseContext.getRequest().isDisableLLM() ? Text2SQLType.ONLY_RULE
                : Text2SQLType.RULE_AND_LLM);
        queryNLReq.setDataSetIds(parseContext.getAgent().getDataSetIds());
        queryNLReq.setChatAppConfig(parseContext.getAgent().getChatAppConfig());

        return queryNLReq;
    }
}
