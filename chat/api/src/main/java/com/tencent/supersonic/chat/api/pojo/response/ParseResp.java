package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import lombok.Data;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResp {
    private Integer chatId;
    private String queryText;
    private Long queryId;
    private ParseState state;
    private List<SemanticParseInfo> selectedParses;
    private List<SemanticParseInfo> candidateParses;
    private List<SolvedQueryRecallResp> similarSolvedQuery;
    private ParseTimeCostDO parseTimeCost;

    public enum ParseState {
        COMPLETED,
        PENDING,
        FAILED
    }
}
