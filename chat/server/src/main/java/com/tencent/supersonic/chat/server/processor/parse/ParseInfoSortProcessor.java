package com.tencent.supersonic.chat.server.processor.parse;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;

import java.util.*;

/**
 * ParseInfoSortProcessor sorts candidate parse info based on certain algorithm. \
 **/
public class ParseInfoSortProcessor implements ParseResultProcessor {

    @Override
    public void process(ParseContext parseContext) {
        Set<String> parseInfoText = Sets.newHashSet();
        List<SemanticParseInfo> sortedParseInfo = Lists.newArrayList();

        parseContext.getResponse().getSelectedParses().forEach(p -> {
            if (!parseInfoText.contains(p.getTextInfo())) {
                sortedParseInfo.add(p);
                parseInfoText.add(p.getTextInfo());
            }
        });

        sortedParseInfo.sort((o1, o2) -> o1.getScore() - o2.getScore() > 0 ? 1 : 0);
        parseContext.getResponse().setSelectedParses(sortedParseInfo);
    }
}
