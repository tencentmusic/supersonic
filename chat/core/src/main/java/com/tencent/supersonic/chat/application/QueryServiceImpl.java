package com.tencent.supersonic.chat.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.api.service.SchemaMapper;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.chat.api.service.SemanticParser;
import com.tencent.supersonic.chat.api.service.SemanticQuery;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.chat.application.query.SemanticQueryFactory;
import com.tencent.supersonic.chat.domain.pojo.chat.QueryData;
import com.tencent.supersonic.chat.domain.service.ChatService;
import com.tencent.supersonic.chat.domain.service.QueryService;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.util.json.JsonUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.stereotype.Service;

@Service
public class QueryServiceImpl implements QueryService {

    private final Logger logger = LoggerFactory.getLogger(QueryServiceImpl.class);
    private List<SchemaMapper> schemaMappers;
    private List<SemanticParser> semanticParsers;
    @Autowired
    private ChatService chatService;
    @Autowired
    private SemanticLayer semanticLayer;

    public QueryServiceImpl() {
        schemaMappers = SpringFactoriesLoader.loadFactories(SchemaMapper.class,
                Thread.currentThread().getContextClassLoader());
        semanticParsers = SpringFactoriesLoader.loadFactories(SemanticParser.class,
                Thread.currentThread().getContextClassLoader());
    }

    public QueryResultResp executeQuery(QueryContextReq queryCtx) throws Exception {
        schemaMappers.stream().forEach(s -> s.map(queryCtx));

        // in order to support multi-turn conversation, we need to consider chat context
        ChatContext chatCtx = chatService.getOrCreateContext(queryCtx.getChatId());

        for (SemanticParser semanticParser : semanticParsers) {
            logger.info("semanticParser processing:{}", JsonUtil.prettyToString(semanticParser));
            boolean isFinish = semanticParser.parse(queryCtx, chatCtx);
            if (isFinish) {
                logger.info("semanticParser is finish ,semanticParser:{}", semanticParser.getClass().getName());
                break;
            }
        }
        // submit semantic query based on the result of semantic parsing
        SemanticQuery query = SemanticQueryFactory.get(queryCtx.getParseInfo().getQueryMode());

        QueryResultResp queryResponse = query.execute(queryCtx, chatCtx);

        // update chat context after a successful semantic query
        query.updateContext(queryResponse, chatCtx, queryCtx);

        chatService.addQuery(queryResponse, queryCtx, chatCtx);

        return queryResponse;
    }

    @Override
    public SemanticParseInfo queryContext(QueryContextReq queryCtx) {
        ChatContext context = chatService.getOrCreateContext(queryCtx.getChatId());
        return context.getParseInfo();
    }

    @Override
    public QueryResultResp queryData(QueryData queryData, User user) throws Exception {
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        QueryResultResp queryResponse = new QueryResultResp();
        BeanUtils.copyProperties(queryData, semanticParseInfo);
        QueryResultWithSchemaResp resultWithColumns = semanticLayer.queryByStruct(
                SchemaInfoConverter.convertTo(semanticParseInfo), user);
        queryResponse.setQueryColumns(resultWithColumns.getColumns());
        queryResponse.setQueryResults(resultWithColumns.getResultList());
        return queryResponse;
    }
}

