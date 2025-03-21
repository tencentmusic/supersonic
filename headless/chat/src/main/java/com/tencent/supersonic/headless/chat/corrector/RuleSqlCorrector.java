package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.supersonic.headless.chat.parser.ParserConfig.PARSER_RULE_CORRECTOR_ENABLE;

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
        ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
        if(!Boolean.parseBoolean(parserConfig.getParameterValue(PARSER_RULE_CORRECTOR_ENABLE))) {
            return;
        }

        for (BaseSemanticCorrector corrector : correctors) {
            corrector.correct(chatQueryContext, semanticParseInfo);
        }
    }
}
