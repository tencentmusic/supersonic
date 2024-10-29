package com.tencent.supersonic.chat.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseTimeCostResp;
import lombok.Data;

import java.util.List;

@Data
public class ChatParseResp {

    private Long queryId;
    private ParseResp.ParseState state = ParseResp.ParseState.PENDING;
    private String errorMsg;
    private List<SemanticParseInfo> selectedParses = Lists.newArrayList();
    private ParseTimeCostResp parseTimeCost = new ParseTimeCostResp();
    private List<Text2SQLExemplar> usedExemplars;

    public ChatParseResp(Long queryId) {
        this.queryId = queryId;
    }

}
