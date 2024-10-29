package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * ParseInfoSortProcessor sorts candidate parse info based on certain algorithm. \
 **/
@Slf4j
public class ParseInfoSortProcessor implements ParseResultProcessor {

    @Override
    public void process(ParseContext parseContext) {
        List<SemanticParseInfo> selectedParses = parseContext.getResponse().getSelectedParses();
        selectedParses.sort(new SemanticParseInfo.SemanticParseComparator());
        // re-assign parseId
        for (int i = 0; i < selectedParses.size(); i++) {
            SemanticParseInfo parseInfo = selectedParses.get(i);
            parseInfo.setId(i + 1);
        }
    }

}
