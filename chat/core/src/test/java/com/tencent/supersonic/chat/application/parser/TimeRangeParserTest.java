package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.parser.rule.TimeRangeParser;
import org.junit.jupiter.api.Test;


class TimeRangeParserTest {

//    private HeuristicQuerySelector voteStrategy = new HeuristicQuerySelector() {
//        @Override
//        public void init(List<SemanticParser> semanticParsers) {
//            List<String> queryMode = new ArrayList<>(Arrays.asList(EntityDetailQuery.QUERY_MODE));
//            for(SemanticParser semanticParser : semanticParsers) {
//                if(semanticParser.getName().equals(TimeRangeParser.PARSER_MODE)) {
//                    semanticParser.getQueryModes().clear();
//                    semanticParser.getQueryModes().addAll(queryMode);
//                }
//            }
//        }
//    };

    @Test
    void parse() {
        TimeRangeParser timeRangeParser = new TimeRangeParser();

        QueryReq queryRequest = new QueryReq();
        ChatContext chatCtx = new ChatContext();
        SchemaMapInfo schemaMap = new SchemaMapInfo();

        queryRequest.setQueryText("supersonic最近30天访问次数");
        //voteStrategy.init(new ArrayList<>(Arrays.asList(timeRangeParser)));
        timeRangeParser.parse(new QueryContext(queryRequest), chatCtx);

        //DateConf dateInfo = queryContext.getParseInfo(timeRangeParser.getQueryModes().get(0))
        //       .getDateInfo();

        //System.out.println(dateInfo);

    }
}