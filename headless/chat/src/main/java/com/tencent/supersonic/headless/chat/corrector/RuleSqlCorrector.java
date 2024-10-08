package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RuleSqlCorrector extends BaseSemanticCorrector {
    private List<BaseSemanticCorrector> correctors;

    public RuleSqlCorrector() {
        correctors = new ArrayList<>();
        correctors.add(new SchemaCorrector());
        correctors.add(new TimeCorrector());
        correctors.add(new GrammarCorrector());
    }

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        for (BaseSemanticCorrector corrector : correctors) {
            corrector.correct(chatQueryContext, semanticParseInfo);
        }
    }
}
