package com.tencent.supersonic.chat.test.context;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.mapper.HanlpSchemaMapper;
import com.tencent.supersonic.chat.application.mapper.QueryMatchStrategy;
import com.tencent.supersonic.chat.application.parser.DomainSemanticParser;
import com.tencent.supersonic.chat.domain.service.ChatService;
import com.tencent.supersonic.chat.test.ChatBizLauncher;
import com.tencent.supersonic.knowledge.infrastructure.nlp.Suggester;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatBizLauncher.class)
public class QueryServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryServiceImplTest.class);

//    @MockBean
//    private QueryService queryService;

    //private SemanticLayer semanticLayer = mock(SemanticLayer.class);

//    @MockBean
//    private DefaultSemanticLayerImpl semanticLayer;
    //SemanticLayer
    @Autowired
    public QueryMatchStrategy queryMatchStrategy;
    @Autowired(required = false)
    public Suggester suggester;
    @MockBean
    private SemanticLayer semanticLayer;//= ComponentFactory.getSemanticLayer();

//    @Before
//    public void setUp() {
//        //List<DomainSchemaResp> getDomainSchemaInfo(List<Long> ids)
//        List<DomainSchemaResp> domainSchemaRespList=MockUtils.getChatContext();
//        Mockito.when(semanticLayer.getDomainSchemaInfo(Mockito.anyList())).thenReturn(domainSchemaRespList);
//    }
    @MockBean
    private DomainSemanticParser domainSemanticParser;
    @MockBean
    private HanlpSchemaMapper hanlpSchemaMapper;
    @MockBean
    private ChatService chatService;

////    @Autowired(required = false)
////    private ChatService chatService;

    //private SemanticLayer semanticLayer ;

    @Test
    public void test() throws Exception {
        QueryContextReq queryContextReq = getQueryContextReq("超音数访问次数");
        //hanlpSchemaMapper.map(queryContextReq);
//        List<Term> terms = HanlpHelper.getSegment().seg(queryContextReq.getQueryText().toLowerCase()).stream()
//                .collect(Collectors.toList());
//        LOGGER.info("terms::::{}",terms);
//        MockUtils.putSuggester();
//        List<MapResult> matches = queryMatchStrategy.match(queryContextReq.getQueryText(), terms, queryContextReq.getDomainId());
//        HanlpHelper.transLetterOriginal(matches);
//        HanlpSchemaMapperHelper.convertTermsToSchemaMapInfo(matches, queryContextReq.getMapInfo());
        HanlpSchemaMapper hanlpSchemaMapper = new HanlpSchemaMapper();
        hanlpSchemaMapper.map(queryContextReq);
        //QueryContextReq queryContextReq=MockUtils.getQueryContextReq("METRIC_FILTER");
        LOGGER.info("QueryContextReq::::{}", queryContextReq.getMapInfo().getMatchedDomains());
        LOGGER.info("QueryContextReq::::{}", queryContextReq.getMapInfo().getDomainElementMatches());
        LOGGER.info("QueryContextReq::::{}", queryContextReq);

//        //chatService=new ChatServiceImpl();
//        ChatContext chatCtx = chatService.getOrCreateContext(queryContextReq.getChatId());
//        if (chatCtx == null) {
//            chatCtx=new ChatContext();
//            chatCtx.setChatId(queryContextReq.getChatId());
//        }
//        LOGGER.info("chatService::::{}",chatService);
//        LOGGER.info("ChatContext::::{}",chatCtx);
        ChatContext chatCtx = new ChatContext();//MockUtils.getChatContext1();
        //semanticLayer = ComponentFactory.getSemanticLayer();
//        DomainSemanticParser domainSemanticParser=new DomainSemanticParser();
//        domainSemanticParser.parse(queryContextReq,chatCtx);

        //DomainSemanticParser

        //domainSemanticParser=new DomainSemanticParser();
        LOGGER.info("domainSemanticParser::::{}", domainSemanticParser);
        //SemanticLayer semanticLayer= mock(SemanticLayer.class);
        //List<DomainSchemaResp> domainSchemaRespList=MockUtils.getChatContext();

//        //SemanticLayer semanticLayer = mock(SemanticLayer.class);
//        when(semanticLayer.getDomainSchemaInfo(Mockito.anyList())).thenReturn(domainSchemaRespList);
//        domainSemanticParser.parse(queryContextReq,chatCtx);
//        LOGGER.info("QueryContextReq::::{}",queryContextReq);
//        TimeSemanticParser timeSemanticParser=new TimeSemanticParser();
//        timeSemanticParser.parse(queryContextReq,chatCtx);
//        AggregateSemanticParser aggregateSemanticParser=new AggregateSemanticParser();
//        aggregateSemanticParser.parse(queryContextReq,chatCtx);
//        //PickStrategy pickStrategy = ComponentFactory.getPickStrategy();
//        LOGGER.info("pickStrategy::::{}",pickStrategy);
//        pickStrategy=new ScoreBasedPickStrategy();
//        SemanticParseInfo semanticParse = pickStrategy.pick(queryContextReq, chatCtx);
//        LOGGER.info("semanticParse::::{}",semanticParse);
        //SemanticQueryExecutorHelper semanticQueryExecutorHelper=new SemanticQueryExecutorHelper();
        //semanticQueryExecutorHelper.execute(queryContextReq.getParseInfo(),queryContextReq.getUser());
//        LOGGER.info("queryContextReq::::{}",queryContextReq.getMapInfo().getMatchedDomains());
//        LOGGER.info("queryContextReq::::{}",queryContextReq.getMapInfo().getDomainElementMatches());
//        LOGGER.info("queryContextReq::::{}",queryContextReq.getCandidateParseInfos());
//        LOGGER.info("queryContextReq::::{}",queryContextReq.getDomainId());

//        List<QueryExecutor> queryExecutors = ComponentFactory.getQueryExecutors();
//        QueryResultResp queryResponse=new QueryResultResp();
//        for (QueryExecutor executor : queryExecutors) {
//             queryResponse = executor.execute(semanticParse, queryContextReq.getUser());
//            if (queryResponse != null) {
//                // update chat context after a successful semantic query
//                if (queryContextReq.isSaveAnswer() && queryResponse.getQueryState() == QueryState.NORMAL.getState()) {
//                    chatService.updateContext(chatCtx, queryContextReq, semanticParse);
//                }
//                queryResponse.setChatContext(chatCtx.getParseInfo());
//                chatService.addQuery(queryResponse, queryContextReq, chatCtx);
//                break;
//            }
//        }

        //assertThat(found.getName()).isEqualTo(name);
    }

    public QueryContextReq getQueryContextReq(String query) {
        QueryContextReq queryContextReq = new QueryContextReq();
        queryContextReq.setQueryText(query);//"alice的访问次数"
        queryContextReq.setChatId(1);
        queryContextReq.setUser(new User(1L, "admin", "admin", "admin@email"));
        return queryContextReq;
    }

    @TestConfiguration
    static class EmployeeServiceImplTestContextConfiguration {

        @Bean
        public QueryMatchStrategy queryMatchStrategyService() {
            return new QueryMatchStrategy();
        }
//        @Bean
//        public SemanticLayer querySemanticLayer() {
//            return new DefaultSemanticLayerImpl();
//        }
        //@Bean
        //public DomainSemanticParser queryDomainSemanticParser() {
        //     return new DomainSemanticParser();
        //}

//        @Bean
//        public RestTemplate getRestTemplate(){
//            return new RestTemplate();
//        }
//
//        @Bean
//        public DefaultSemanticInternalUtils getUtils(){
//            return new DefaultSemanticInternalUtils();
//        }

    }
}
