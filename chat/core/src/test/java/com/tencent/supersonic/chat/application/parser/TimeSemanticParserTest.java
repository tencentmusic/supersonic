package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import org.junit.jupiter.api.Test;


class TimeSemanticParserTest {

//    private HeuristicQuerySelector voteStrategy = new HeuristicQuerySelector() {
//        @Override
//        public void init(List<SemanticParser> semanticParsers) {
//            List<String> queryMode = new ArrayList<>(Arrays.asList(EntityDetail.QUERY_MODE));
//            for(SemanticParser semanticParser : semanticParsers) {
//                if(semanticParser.getName().equals(TimeSemanticParser.PARSER_MODE)) {
//                    semanticParser.getQueryModes().clear();
//                    semanticParser.getQueryModes().addAll(queryMode);
//                }
//            }
//        }
//    };

    @Test
    void parse() {
        TimeSemanticParser timeSemanticParser = new TimeSemanticParser();

        QueryContextReq queryContext = new QueryContextReq();
        ChatContext chatCtx = new ChatContext();
        SchemaMapInfo schemaMap = new SchemaMapInfo();

        queryContext.setQueryText("supersonic最近30天访问次数");
        //voteStrategy.init(new ArrayList<>(Arrays.asList(timeSemanticParser)));
        timeSemanticParser.parse(queryContext, chatCtx);

        //DateConf dateInfo = queryContext.getParseInfo(timeSemanticParser.getQueryModes().get(0))
        //       .getDateInfo();

        //System.out.println(dateInfo);

    }
}