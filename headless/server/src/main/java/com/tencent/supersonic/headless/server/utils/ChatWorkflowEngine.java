package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.enums.ChatWorkflowState;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.corrector.SemanticCorrector;
import com.tencent.supersonic.headless.chat.mapper.SchemaMapper;
import com.tencent.supersonic.headless.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.processor.ResultProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatWorkflowEngine {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");
    private List<SchemaMapper> schemaMappers = ComponentFactory.getSchemaMappers();
    private List<SemanticParser> semanticParsers = ComponentFactory.getSemanticParsers();
    private List<SemanticCorrector> semanticCorrectors = ComponentFactory.getSemanticCorrectors();
    private List<ResultProcessor> resultProcessors = ComponentFactory.getResultProcessors();

    public void execute(ChatQueryContext queryCtx, ChatContext chatCtx, ParseResp parseResult) {
        queryCtx.setChatWorkflowState(ChatWorkflowState.MAPPING);
        while (queryCtx.getChatWorkflowState() != ChatWorkflowState.FINISHED) {
            switch (queryCtx.getChatWorkflowState()) {
                case MAPPING:
                    performMapping(queryCtx);
                    queryCtx.setChatWorkflowState(ChatWorkflowState.PARSING);
                    break;
                case PARSING:
                    performParsing(queryCtx, chatCtx);
                    queryCtx.setChatWorkflowState(ChatWorkflowState.CORRECTING);
                    break;
                case CORRECTING:
                    performCorrecting(queryCtx);
                    queryCtx.setChatWorkflowState(ChatWorkflowState.TRANSLATING);
                    break;
                case TRANSLATING:
                    long start = System.currentTimeMillis();
                    performTranslating(queryCtx);
                    parseResult.getParseTimeCost().setSqlTime(System.currentTimeMillis() - start);
                    queryCtx.setChatWorkflowState(ChatWorkflowState.PROCESSING);
                    break;
                case PROCESSING:
                default:
                    performProcessing(queryCtx, chatCtx, parseResult);
                    queryCtx.setChatWorkflowState(ChatWorkflowState.FINISHED);
                    break;
            }
        }
    }

    public void performMapping(ChatQueryContext queryCtx) {
        if (Objects.isNull(queryCtx.getMapInfo())
                || MapUtils.isEmpty(queryCtx.getMapInfo().getDataSetElementMatches())) {
            schemaMappers.forEach(mapper -> mapper.map(queryCtx));
        }
    }

    public void performParsing(ChatQueryContext queryCtx, ChatContext chatCtx) {
        semanticParsers.forEach(parser -> {
            parser.parse(queryCtx, chatCtx);
            log.debug("{} result:{}", parser.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });
    }

    public void performCorrecting(ChatQueryContext queryCtx) {
        List<SemanticQuery> candidateQueries = queryCtx.getCandidateQueries();
        if (CollectionUtils.isNotEmpty(candidateQueries)) {
            for (SemanticQuery semanticQuery : candidateQueries) {
                if (semanticQuery instanceof RuleSemanticQuery) {
                    continue;
                }
                for (SemanticCorrector corrector : semanticCorrectors) {
                    corrector.correct(queryCtx, semanticQuery.getParseInfo());
                    if (!ChatWorkflowState.CORRECTING.equals(queryCtx.getChatWorkflowState())) {
                        break;
                    }
                }
            }
        }
    }

    public void performProcessing(ChatQueryContext queryCtx, ChatContext chatCtx, ParseResp parseResult) {
        resultProcessors.forEach(processor -> {
            processor.process(parseResult, queryCtx, chatCtx);
        });
    }

    private void performTranslating(ChatQueryContext chatQueryContext) {
        List<SemanticParseInfo> semanticParseInfos = chatQueryContext.getCandidateQueries().stream()
                .map(SemanticQuery::getParseInfo)
                .collect(Collectors.toList());

        semanticParseInfos.forEach(parseInfo -> {
            try {
                SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
                if (Objects.isNull(semanticQuery)) {
                    return;
                }
                semanticQuery.setParseInfo(parseInfo);
                SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
                SemanticLayerService queryService = ContextUtils.getBean(SemanticLayerService.class);
                SemanticTranslateResp explain = queryService.translate(semanticQueryReq, chatQueryContext.getUser());
                parseInfo.getSqlInfo().setQuerySQL(explain.getQuerySQL());

                keyPipelineLog.info("SqlInfoProcessor results:\n"
                                + "Parsed S2SQL: {}\nCorrected S2SQL: {}\nQuery SQL: {}",
                        StringUtils.normalizeSpace(parseInfo.getSqlInfo().getParsedS2SQL()),
                        StringUtils.normalizeSpace(parseInfo.getSqlInfo().getCorrectedS2SQL()),
                        StringUtils.normalizeSpace(parseInfo.getSqlInfo().getQuerySQL()));
            } catch (Exception e) {
                log.warn("get sql info failed:{}", parseInfo, e);
            }
        });
    }
}