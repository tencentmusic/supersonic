package com.tencent.supersonic.chat.parser.sql.llm;

import com.tencent.supersonic.chat.agent.NL2SQLTool;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.ModelCluster;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class LLMSqlParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryCtx, ChatContext chatCtx) {
        QueryReq request = queryCtx.getRequest();
        LLMRequestService requestService = ContextUtils.getBean(LLMRequestService.class);
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        //1.determine whether to skip this parser.
        if (requestService.isSkip(queryCtx)) {
            return;
        }
        try {
            //2.get modelId from queryCtx and chatCtx.
            ModelCluster modelCluster = requestService.getModelCluster(queryCtx, chatCtx, request.getAgentId());
            if (StringUtils.isBlank(modelCluster.getKey())) {
                return;
            }
            //3.get agent tool and determine whether to skip this parser.
            NL2SQLTool commonAgentTool = requestService.getParserTool(request, modelCluster.getModelIds());
            if (Objects.isNull(commonAgentTool)) {
                log.info("no tool in this agent, skip {}", LLMSqlParser.class);
                return;
            }
            //4.construct a request, call the API for the large model, and retrieve the results.
            List<ElementValue> linkingValues = requestService.getValueList(queryCtx, modelCluster);
            SemanticSchema semanticSchema = semanticService.getSemanticSchema();
            LLMReq llmReq = requestService.getLlmReq(queryCtx, semanticSchema, modelCluster, linkingValues);
            LLMResp llmResp = requestService.requestLLM(llmReq, modelCluster.getKey());

            if (Objects.isNull(llmResp)) {
                return;
            }
            //5. deduplicate the SQL result list and build parserInfo
            modelCluster.buildName(semanticSchema.getModelIdToName());
            LLMResponseService responseService = ContextUtils.getBean(LLMResponseService.class);
            Map<String, Double> deduplicationSqlWeight = responseService.getDeduplicationSqlWeight(llmResp);
            ParseResult parseResult = ParseResult.builder()
                    .request(request)
                    .modelCluster(modelCluster)
                    .commonAgentTool(commonAgentTool)
                    .llmReq(llmReq)
                    .llmResp(llmResp)
                    .linkingValues(linkingValues)
                    .build();

            if (MapUtils.isEmpty(deduplicationSqlWeight)) {
                responseService.addParseInfo(queryCtx, parseResult, llmResp.getSqlOutput(), 1D);
            } else {
                deduplicationSqlWeight.forEach((sql, weight) -> {
                    responseService.addParseInfo(queryCtx, parseResult, sql, weight);
                });
            }

        } catch (Exception e) {
            log.error("parse", e);
        }
    }

}
