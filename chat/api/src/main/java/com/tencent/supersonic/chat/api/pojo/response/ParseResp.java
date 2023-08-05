package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import lombok.*;

import java.util.List;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResp {
    private Integer chatId;
    private String queryText;
    private ParseState state;
    private List<SemanticParseInfo> selectedParses;
    private List<SemanticParseInfo> candidateParses;

    public enum ParseState {
        COMPLETED,
        PENDING,
        FAILED
    }
}
