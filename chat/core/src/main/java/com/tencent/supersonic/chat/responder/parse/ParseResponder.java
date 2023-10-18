package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import java.util.List;

public interface ParseResponder {

    void fillResponse(ParseResp parseResp, QueryContext queryContext, List<ChatParseDO> chatParseDOS);

}