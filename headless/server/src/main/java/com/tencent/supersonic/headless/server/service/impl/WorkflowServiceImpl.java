package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.enums.WorkflowState;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.corrector.SemanticCorrector;
import com.tencent.supersonic.headless.chat.mapper.SchemaMapper;
import com.tencent.supersonic.headless.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.headless.server.processor.ResultProcessor;
import com.tencent.supersonic.headless.server.service.WorkflowService;
import com.tencent.supersonic.headless.server.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class WorkflowServiceImpl implements WorkflowService {
    private List<SchemaMapper> schemaMappers = ComponentFactory.getSchemaMappers();
    private List<SemanticParser> semanticParsers = ComponentFactory.getSemanticParsers();
    private List<SemanticCorrector> semanticCorrectors = ComponentFactory.getSemanticCorrectors();
    private List<ResultProcessor> resultProcessors = ComponentFactory.getResultProcessors();

    public void startWorkflow(QueryContext queryCtx, ChatContext chatCtx, ParseResp parseResult) {
        queryCtx.setWorkflowState(WorkflowState.MAPPING);
        while (queryCtx.getWorkflowState() != WorkflowState.FINISHED) {
            switch (queryCtx.getWorkflowState()) {
                case MAPPING:
                    performMapping(queryCtx);
                    queryCtx.setWorkflowState(WorkflowState.PARSING);
                    break;
                case PARSING:
                    performParsing(queryCtx, chatCtx);
                    queryCtx.setWorkflowState(WorkflowState.CORRECTING);
                    break;
                case CORRECTING:
                    performCorrecting(queryCtx);
                    queryCtx.setWorkflowState(WorkflowState.PROCESSING);
                    break;
                case PROCESSING:
                default:
                    performProcessing(queryCtx, chatCtx, parseResult);
                    queryCtx.setWorkflowState(WorkflowState.FINISHED);
                    break;
            }
        }
    }

    public void performMapping(QueryContext queryCtx) {
        if (Objects.isNull(queryCtx.getMapInfo())
                || MapUtils.isEmpty(queryCtx.getMapInfo().getDataSetElementMatches())) {
            schemaMappers.forEach(mapper -> mapper.map(queryCtx));
        }
    }

    public void performParsing(QueryContext queryCtx, ChatContext chatCtx) {
        semanticParsers.forEach(parser -> {
            parser.parse(queryCtx, chatCtx);
            log.debug("{} result:{}", parser.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });
    }

    public void performCorrecting(QueryContext queryCtx) {
        List<SemanticQuery> candidateQueries = queryCtx.getCandidateQueries();
        if (CollectionUtils.isNotEmpty(candidateQueries)) {
            for (SemanticQuery semanticQuery : candidateQueries) {
                if (semanticQuery instanceof RuleSemanticQuery) {
                    continue;
                }
                for (SemanticCorrector corrector : semanticCorrectors) {
                    corrector.correct(queryCtx, semanticQuery.getParseInfo());
                    if (!WorkflowState.CORRECTING.equals(queryCtx.getWorkflowState())) {
                        break;
                    }
                }
            }
        }
    }

    public void performProcessing(QueryContext queryCtx, ChatContext chatCtx, ParseResp parseResult) {
        resultProcessors.forEach(processor -> {
            processor.process(parseResult, queryCtx, chatCtx);
        });
    }
}