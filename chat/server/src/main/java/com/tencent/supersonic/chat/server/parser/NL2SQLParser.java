package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.core.chat.mapper.SchemaMapper;
import com.tencent.supersonic.headless.core.chat.parser.llm.LLMRequestService;
import com.tencent.supersonic.headless.core.chat.parser.llm.RewriteQueryGeneration;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.server.service.ChatQueryService;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import java.util.stream.Collectors;


import com.tencent.supersonic.headless.server.service.impl.ChatQueryServiceImpl;
import com.tencent.supersonic.headless.server.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import static com.tencent.supersonic.common.pojo.Constants.CONTEXT;

@Slf4j
public class NL2SQLParser implements ChatParser {

    private int contextualNum = 5;

    private List<SchemaMapper> schemaMappers = ComponentFactory.getSchemaMappers();

    @Override
    public void parse(ChatParseContext chatParseContext, ParseResp parseResp) {
        if (!chatParseContext.enableNL2SQL()) {
            return;
        }
        if (checkSkip(parseResp)) {
            return;
        }
        considerMultiturn(chatParseContext, parseResp);
        QueryReq queryReq = QueryReqConverter.buildText2SqlQueryReq(chatParseContext);
        queryReq.setMapInfo(null);
        ChatQueryService chatQueryService = ContextUtils.getBean(ChatQueryService.class);
        ParseResp text2SqlParseResp = chatQueryService.performParsing(queryReq);
        if (!ParseResp.ParseState.FAILED.equals(text2SqlParseResp.getState())) {
            parseResp.getSelectedParses().addAll(text2SqlParseResp.getSelectedParses());
        }
        parseResp.getParseTimeCost().setSqlTime(text2SqlParseResp.getParseTimeCost().getSqlTime());
    }

    private void considerMultiturn(ChatParseContext chatParseContext, ParseResp parseResp) {
        Long startTime = System.currentTimeMillis();
        Environment environment = ContextUtils.getBean(Environment.class);
        RewriteQueryGeneration rewriteQueryGeneration = ContextUtils.getBean(RewriteQueryGeneration.class);
        String multiTurn = environment.getProperty("multi.turn");
        String multiNum = environment.getProperty("multi.num");
        if (StringUtils.isNotBlank(multiTurn) && Boolean.parseBoolean(multiTurn)) {
            log.info("multi turn text-to-sql!");
            ChatQueryRepository chatQueryRepository = ContextUtils.getBean(ChatQueryRepository.class);
            List<SemanticParseInfo> contextualParseInfoList = chatQueryRepository.getContextualParseInfo(
                    parseResp.getChatId()).stream().filter(o -> o.getQueryMode().equals(LLMSqlQuery.QUERY_MODE)
            ).collect(Collectors.toList());
            if (StringUtils.isNotBlank(multiNum) && StringUtils.isNumeric(multiNum)) {
                int num = Integer.parseInt(multiNum);
                contextualNum = Math.min(contextualNum, num);
            }
            List<SemanticParseInfo> contextualList = contextualParseInfoList.subList(0,
                    Math.min(contextualNum, contextualParseInfoList.size()));
            Collections.reverse(contextualList);
            List<String> contextualQuestions = new ArrayList<>();
            contextualList.stream().forEach(o -> {
                Map<String, Object> map = JsonUtil.toMap(JsonUtil.toString(
                        o.getProperties().get(CONTEXT)), String.class, Object.class);
                LLMReq llmReq = JsonUtil.toObject(JsonUtil.toString(map.get("llmReq")), LLMReq.class);
                List<LLMReq.ElementValue> linking = llmReq.getLinking();
                List<String> priorLinkingList = new ArrayList<>();
                for (LLMReq.ElementValue priorLinking : linking) {
                    String fieldName = priorLinking.getFieldName();
                    String fieldValue = priorLinking.getFieldValue();
                    priorLinkingList.add("‘" + fieldValue + "‘是一个‘" + fieldName + "‘");
                }
                String linkingListStr = String.join("，", priorLinkingList);
                String questionAugmented = String.format("%s (补充信息:%s) ", llmReq.getQueryText(), linkingListStr);
                contextualQuestions.add(questionAugmented);
            });
            Long startTime1 = System.currentTimeMillis();
            log.info("timecost1:{}", startTime1 - startTime);
            StringBuffer currentPromptSb = new StringBuffer();
            currentPromptSb.append("contextualQuestions:" + JsonUtil.toString(contextualQuestions) + "\n");
            String currentQuestion = getQueryLinks(chatParseContext);
            currentPromptSb.append("currentQuestion:" + currentQuestion + "\n");
            currentPromptSb.append("rewritingCurrentQuestion:\n");
            Long startTime2 = System.currentTimeMillis();
            log.info("timecost2:{}", startTime2 - startTime1);
            String rewriteQuery = rewriteQueryGeneration.generation(currentPromptSb.toString());
            Long startTime3 = System.currentTimeMillis();
            log.info("timecost3:{}", startTime3 - startTime2);
            log.info("rewriteQuery:{}", rewriteQuery);
            chatParseContext.setQueryText(rewriteQuery);
        }
    }

    private String getQueryLinks(ChatParseContext chatParseContext) {
        //ChatContextService chatContextService=ContextUtils.getBean(ChatContextService.class);
        QueryReq queryReq = QueryReqConverter.buildText2SqlQueryReq(chatParseContext);

        ChatQueryServiceImpl chatQueryService = ContextUtils.getBean(ChatQueryServiceImpl.class);
        // build queryContext and chatContext
        QueryContext queryCtx = chatQueryService.buildQueryContext(queryReq);

        // 1. mapper
        if (Objects.isNull(chatParseContext.getMapInfo())
                || MapUtils.isEmpty(chatParseContext.getMapInfo().getDataSetElementMatches())) {
            schemaMappers.forEach(mapper -> {
                mapper.map(queryCtx);
            });
        }
        LLMRequestService requestService = ContextUtils.getBean(LLMRequestService.class);
        log.info("LLMRequestService:{}", requestService.getClass().getSimpleName());
        Long dataSetId = requestService.getDataSetId(queryCtx);
        log.info("dataSetId:{}", dataSetId);
        if (dataSetId == null) {
            return null;
        }
        List<LLMReq.ElementValue> linkingValues = requestService.getValueList(queryCtx, dataSetId);
        List<String> priorLinkingList = new ArrayList<>();
        for (LLMReq.ElementValue priorLinking : linkingValues) {
            String fieldName = priorLinking.getFieldName();
            String fieldValue = priorLinking.getFieldValue();
            priorLinkingList.add("‘" + fieldValue + "‘是一个‘" + fieldName + "‘");
        }
        String linkingListStr = String.join("，", priorLinkingList);
        String questionAugmented = String.format("%s (补充信息:%s) ", chatParseContext.getQueryText(), linkingListStr);
        log.info("questionAugmented:{}", questionAugmented);
        return questionAugmented;
    }

    private boolean checkSkip(ParseResp parseResp) {
        List<SemanticParseInfo> selectedParses = parseResp.getSelectedParses();
        for (SemanticParseInfo semanticParseInfo : selectedParses) {
            if (semanticParseInfo.getScore() >= parseResp.getQueryText().length()) {
                return true;
            }
        }
        return false;
    }

}
