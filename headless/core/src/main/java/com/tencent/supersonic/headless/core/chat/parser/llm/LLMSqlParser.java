package com.tencent.supersonic.headless.core.chat.parser.llm;


import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.core.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMSqlResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * LLMSqlParser uses large language model to understand query semantics and
 * generate S2SQL statements to be executed by the semantic query engine.
 */
@Slf4j
public class LLMSqlParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryCtx, ChatContext chatCtx) {
        LLMRequestService requestService = ContextUtils.getBean(LLMRequestService.class);
        //1.determine whether to skip this parser.
        if (requestService.isSkip(queryCtx)) {
            return;
        }
        try {
            //2.get dataSetId from queryCtx and chatCtx.
            Long dataSetId = requestService.getDataSetId(queryCtx);
            if (dataSetId == null) {
                return;
            }
            log.info("Generate query statement for dataSetId:{}", dataSetId);

            //3.invoke LLM service to do parsing.
            List<LLMReq.ElementValue> linkingValues = requestService.getValueList(queryCtx, dataSetId);
            SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
            LLMReq llmReq = requestService.getLlmReq(queryCtx, dataSetId, semanticSchema, linkingValues);
            LLMResp llmResp = requestService.invokeLLM(llmReq);
            if (Objects.isNull(llmResp)) {
                return;
            }

            //4. deduplicate the S2SQL result list and build parserInfo
            LLMResponseService responseService = ContextUtils.getBean(LLMResponseService.class);
            Map<String, LLMSqlResp> deduplicationSqlResp = responseService.getDeduplicationSqlResp(llmResp);
            ParseResult parseResult = ParseResult.builder()
                    .dataSetId(dataSetId)
                    .llmReq(llmReq)
                    .llmResp(llmResp)
                    .linkingValues(linkingValues)
                    .build();

            if (MapUtils.isEmpty(deduplicationSqlResp)) {
                if (StringUtils.isNotBlank(llmResp.getSqlOutput())) {
                    responseService.addParseInfo(queryCtx, parseResult, llmResp.getSqlOutput(), 1D);
                }
            } else {
                deduplicationSqlResp.forEach((sql, sqlResp) -> {
                    if (StringUtils.isNotBlank(sql)) {
                        responseService.addParseInfo(queryCtx, parseResult, sql, sqlResp.getSqlWeight());
                    }
                });
            }

        } catch (Exception e) {
            log.error("Failed to parse query:", e);
        }
    }

}
