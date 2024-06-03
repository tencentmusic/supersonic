package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.agent.MultiTurnConfig;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.LLMConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.core.utils.S2ChatModelProvider;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

import static com.tencent.supersonic.chat.server.parser.ParserConfig.PARSER_MULTI_TURN_ENABLE;

@Slf4j
public class MultiTurnParser implements ChatParser {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    private static final PromptTemplate promptTemplate = PromptTemplate.from(
            "You are a data product manager experienced in data requirements."
                    + "Your will be provided with current and history questions asked by a user,"
                    + "along with their mapped schema elements(metric, dimension and value), "
                    + "please try understanding the semantics and rewrite a question"
                    + "(keep relevant entities, metrics, dimensions, values and date ranges)."
                    + "Current Question: {{curtQuestion}} "
                    + "Current Mapped Schema: {{curtSchema}} "
                    + "History Question: {{histQuestion}} "
                    + "History Mapped Schema: {{histSchema}} "
                    + "Rewritten Question: ");

    @Override
    public void parse(ChatParseContext chatParseContext, ParseResp parseResp) {
        ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
        MultiTurnConfig agentMultiTurnConfig = chatParseContext.getAgent().getMultiTurnConfig();
        Boolean globalMultiTurnConfig = Boolean.valueOf(parserConfig.getParameterValue(PARSER_MULTI_TURN_ENABLE));

        Boolean multiTurnConfig = agentMultiTurnConfig != null
                ? agentMultiTurnConfig.isEnableMultiTurn() : globalMultiTurnConfig;
        if (!Boolean.TRUE.equals(multiTurnConfig)) {
            return;
        }

        // derive mapping result of current question and parsing result of last question.
        ChatQueryService chatQueryService = ContextUtils.getBean(ChatQueryService.class);
        QueryReq queryReq = QueryReqConverter.buildText2SqlQueryReq(chatParseContext);
        MapResp currentMapResult = chatQueryService.performMapping(queryReq);

        List<ParseResp> historyParseResults = getHistoryParseResult(chatParseContext.getChatId(), 1);
        if (historyParseResults.size() == 0) {
            return;
        }
        ParseResp lastParseResult = historyParseResults.get(0);
        Long dataId = lastParseResult.getSelectedParses().get(0).getDataSetId();

        String curtMapStr = generateSchemaPrompt(currentMapResult.getMapInfo().getMatchedElements(dataId));
        String histMapStr = generateSchemaPrompt(lastParseResult.getSelectedParses().get(0).getElementMatches());
        String rewrittenQuery = rewriteQuery(RewriteContext.builder()
                        .curtQuestion(currentMapResult.getQueryText())
                        .histQuestion(lastParseResult.getQueryText())
                        .curtSchema(curtMapStr)
                        .histSchema(histMapStr)
                        .llmConfig(queryReq.getLlmConfig())
                        .build());
        chatParseContext.setQueryText(rewrittenQuery);
        log.info("Last Query: {} Current Query: {}, Rewritten Query: {}",
                lastParseResult.getQueryText(), currentMapResult.getQueryText(), rewrittenQuery);
    }

    private String rewriteQuery(RewriteContext context) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("curtQuestion", context.getCurtQuestion());
        variables.put("histQuestion", context.getHistQuestion());
        variables.put("curtSchema", context.getCurtSchema());
        variables.put("histSchema", context.getHistSchema());

        Prompt prompt = promptTemplate.apply(variables);
        keyPipelineLog.info("MultiTurnParser reqPrompt:{}", prompt.toSystemMessage());

        ChatLanguageModel chatLanguageModel = S2ChatModelProvider.provide(context.getLlmConfig());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toSystemMessage());

        String result = response.content().text();
        keyPipelineLog.info("MultiTurnParser modelResp:{}", result);
        return response.content().text();
    }

    private String generateSchemaPrompt(List<SchemaElementMatch> elementMatches) {
        List<String> metrics = new ArrayList<>();
        List<String> dimensions = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (SchemaElementMatch match : elementMatches) {
            if (match.getElement().getType().equals(SchemaElementType.METRIC)) {
                metrics.add(match.getWord());
            } else if (match.getElement().getType().equals(SchemaElementType.DIMENSION)) {
                dimensions.add(match.getWord());
            } else if (match.getElement().getType().equals(SchemaElementType.VALUE)) {
                values.add(match.getWord());
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("'metrics:':[%s]", String.join(",", metrics)));
        prompt.append(",");
        prompt.append(String.format("'dimensions:':[%s]", String.join(",", dimensions)));
        prompt.append(",");
        prompt.append(String.format("'values:':[%s]", String.join(",", values)));

        return prompt.toString();
    }

    private List<ParseResp> getHistoryParseResult(int chatId, int multiNum) {
        ChatQueryRepository chatQueryRepository = ContextUtils.getBean(ChatQueryRepository.class);
        List<ParseResp> contextualParseInfoList = chatQueryRepository.getContextualParseInfo(chatId)
                .stream().filter(p -> p.getState() != ParseResp.ParseState.FAILED).collect(Collectors.toList());

        List<ParseResp> contextualList = contextualParseInfoList.subList(0,
                Math.min(multiNum, contextualParseInfoList.size()));
        Collections.reverse(contextualList);
        return contextualList;
    }

    @Data
    @Builder
    public static class RewriteContext {
        private String curtQuestion;
        private String histQuestion;
        private String curtSchema;
        private String histSchema;
        private LLMConfig llmConfig;
    }
}
