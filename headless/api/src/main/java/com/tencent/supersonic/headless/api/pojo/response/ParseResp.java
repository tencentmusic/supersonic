package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ParseResp {
    private Integer chatId;
    private String queryText;
    private Long queryId;
    private ParseState state;
    private List<SemanticParseInfo> selectedParses = Lists.newArrayList();
    private ParseTimeCostResp parseTimeCost = new ParseTimeCostResp();

    public enum ParseState {
        COMPLETED,
        PENDING,
        FAILED
    }

    public ParseResp(Integer chatId, String queryText) {
        this.chatId = chatId;
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

    public ParseState getState() {
        if (CollectionUtils.isNotEmpty(selectedParses)) {
            this.state = ParseResp.ParseState.COMPLETED;
        } else {
            this.state = ParseState.FAILED;
        }
        return this.state;
    }

    private void generateParseInfoId(List<SemanticParseInfo> selectedParses) {
        for (int i = 0; i < selectedParses.size(); i++) {
            SemanticParseInfo parseInfo = selectedParses.get(i);
            parseInfo.setId(i + 1);
        }
    }

}
