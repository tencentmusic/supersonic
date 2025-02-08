package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ParseResp implements Serializable {
    private final String queryText;
    private ParseState state = ParseState.PENDING;
    private String errorMsg;
    private List<SemanticParseInfo> selectedParses = Lists.newArrayList();
    private ParseTimeCostResp parseTimeCost = new ParseTimeCostResp();

    public enum ParseState {
        COMPLETED, PENDING, FAILED
    }

    public ParseResp(String queryText) {
        this.queryText = queryText;
        parseTimeCost.setParseStartTime(System.currentTimeMillis());
    }

    public List<SemanticParseInfo> getSelectedParses() {
        selectedParses = selectedParses.stream()
                .sorted(Comparator.comparingDouble(SemanticParseInfo::getScore).reversed())
                .collect(Collectors.toList());
        generateParseInfoId(selectedParses);
        return selectedParses;
    }

    private void generateParseInfoId(List<SemanticParseInfo> selectedParses) {
        for (int i = 0; i < selectedParses.size(); i++) {
            SemanticParseInfo parseInfo = selectedParses.get(i);
            parseInfo.setId(i + 1);
        }
    }
}
