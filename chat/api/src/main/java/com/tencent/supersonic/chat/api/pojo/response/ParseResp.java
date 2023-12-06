package com.tencent.supersonic.chat.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import lombok.Data;
import java.util.List;

@Data
public class ParseResp {
    private Integer chatId;
    private String queryText;
    private Long queryId;
    private ParseState state;
    private List<SemanticParseInfo> selectedParses = Lists.newArrayList();
    private List<SemanticParseInfo> candidateParses = Lists.newArrayList();
    private ParseTimeCostDO parseTimeCost = new ParseTimeCostDO();

    public enum ParseState {
        COMPLETED,
        PENDING,
        FAILED
    }

}
