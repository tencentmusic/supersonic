package com.tencent.supersonic.headless.server.processor;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.enums.QueryMethod;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.server.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SqlInfoProcessor adds S2SQL to the parsing results so that
 * technical users could verify SQL by themselves.
 **/
@Slf4j
public class SqlInfoProcessor implements ResultProcessor {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        long start = System.currentTimeMillis();
        List<SemanticQuery> semanticQueries = queryContext.getCandidateQueries();
        if (CollectionUtils.isEmpty(semanticQueries)) {
            return;
        }
        List<SemanticParseInfo> selectedParses = semanticQueries.stream().map(SemanticQuery::getParseInfo)
                .collect(Collectors.toList());
        addSqlInfo(queryContext, selectedParses);
        parseResp.getParseTimeCost().setSqlTime(System.currentTimeMillis() - start);
    }

    private void addSqlInfo(QueryContext queryContext, List<SemanticParseInfo> semanticParseInfos) {
        if (CollectionUtils.isEmpty(semanticParseInfos)) {
            return;
        }
        semanticParseInfos.forEach(parseInfo -> {
            try {
                addSqlInfo(queryContext, parseInfo);
            } catch (Exception e) {
                log.warn("get sql info failed:{}", parseInfo, e);
            }
        });
    }

    private void addSqlInfo(QueryContext queryContext, SemanticParseInfo parseInfo) throws Exception {
        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        if (Objects.isNull(semanticQuery)) {
            return;
        }
        semanticQuery.setParseInfo(parseInfo);
        SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
        SemanticLayerService queryService = ContextUtils.getBean(SemanticLayerService.class);
        ExplainSqlReq<Object> explainSqlReq = ExplainSqlReq.builder().queryReq(semanticQueryReq)
                .queryTypeEnum(QueryMethod.SQL).build();
        ExplainResp explain = queryService.explain(explainSqlReq, queryContext.getUser());
        String explainSql = explain.getSql();
        if (StringUtils.isBlank(explainSql)) {
            return;
        }
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (semanticQuery instanceof LLMSqlQuery) {
            keyPipelineLog.info("SqlInfoProcessor results:\nParsed S2SQL:{}\nCorrected S2SQL:{}\nFinal SQL:{}",
                    sqlInfo.getS2SQL(), sqlInfo.getCorrectS2SQL(), explainSql);
        }
        sqlInfo.setQuerySQL(explainSql);
    }

}
