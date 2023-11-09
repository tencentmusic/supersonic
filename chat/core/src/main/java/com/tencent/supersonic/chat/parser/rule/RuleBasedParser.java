package com.tencent.supersonic.chat.parser.rule;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class RuleBasedParser implements SemanticParser {

    private static List<SemanticParser> ruleParsers = Arrays.asList(
            new QueryModeParser(),
            new ContextInheritParser(),
            new AgentCheckParser(),
            new MetricCheckParser(),
            new TimeRangeParser(),
            new AggregateTypeParser()
    );

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        ruleParsers.stream().forEach(p -> p.parse(queryContext, chatContext));
    }
}
