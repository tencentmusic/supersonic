package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.agent.MultiTurnConfig;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.S2ChatModelProvider;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.server.facade.service.ChatQueryService;
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
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.chat.server.parser.ParserConfig.PARSER_MULTI_TURN_ENABLE;

@Slf4j
public class NL2SQLParser implements ChatParser {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    private static final String REWRITE_INSTRUCTION = ""
            + "#Role: You are a data product manager experienced in data requirements.\n"
            + "#Task: Your will be provided with current and history questions asked by a user,"
            + "along with their mapped schema elements(metric, dimension and value),"
            + "please try understanding the semantics and rewrite a question.\n"
            + "#Rules: "
            + "1.ALWAYS keep relevant entities, metrics, dimensions, values and date ranges. "
            + "2.ONLY respond with the rewritten question.\n"
            + "#Current Question: %s\n"
            + "#Current Mapped Schema: %s\n"
            + "#History Question: %s\n"
            + "#History Mapped Schema: %s\n"
            + "#History SQL: %s\n"
            + "#Rewritten Question: ";

    @Override
    public void parse(ChatParseContext chatParseContext, ParseResp parseResp) {
        if (!chatParseContext.enableNL2SQL() || checkSkip(parseResp)) {
            return;
        }
        processMultiTurn(chatParseContext, parseResp);

        QueryReq queryReq = QueryReqConverter.buildText2SqlQueryReq(chatParseContext);
        ChatQueryService chatQueryService = ContextUtils.getBean(ChatQueryService.class);
        ParseResp text2SqlParseResp = chatQueryService.performParsing(queryReq);
        if (!ParseResp.ParseState.FAILED.equals(text2SqlParseResp.getState())) {
            parseResp.getSelectedParses().addAll(text2SqlParseResp.getSelectedParses());
        }
        parseResp.getParseTimeCost().setSqlTime(text2SqlParseResp.getParseTimeCost().getSqlTime());
        formatParseResult(parseResp);
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

    private void formatParseResult(ParseResp parseResp) {
        List<SemanticParseInfo> selectedParses = parseResp.getSelectedParses();
        for (SemanticParseInfo parseInfo : selectedParses) {
            formatParseInfo(parseInfo);
        }
    }

    private void formatParseInfo(SemanticParseInfo parseInfo) {
        if (!PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            formatNL2SQLParseInfo(parseInfo);
        }
    }

    private void formatNL2SQLParseInfo(SemanticParseInfo parseInfo) {
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("**数据集:** ").append(parseInfo.getDataSet().getName()).append(" ");
        Optional<SchemaElement> metric = parseInfo.getMetrics().stream().findFirst();
        metric.ifPresent(schemaElement ->
                textBuilder.append("**指标:** ").append(schemaElement.getName()).append(" "));
        List<String> dimensionNames = parseInfo.getDimensions().stream()
                .map(SchemaElement::getName).filter(Objects::nonNull).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(dimensionNames)) {
            textBuilder.append("**维度:** ").append(String.join(",", dimensionNames));
        }
        textBuilder.append("\n\n**筛选条件:** \n");
        if (parseInfo.getDateInfo() != null) {
            textBuilder.append("**数据时间:** ").append(parseInfo.getDateInfo().getStartDate()).append("~")
                    .append(parseInfo.getDateInfo().getEndDate()).append(" ");
        }
        if (!CollectionUtils.isEmpty(parseInfo.getDimensionFilters())
                || CollectionUtils.isEmpty(parseInfo.getMetricFilters())) {
            Set<QueryFilter> queryFilters = parseInfo.getDimensionFilters();
            queryFilters.addAll(parseInfo.getMetricFilters());
            for (QueryFilter queryFilter : queryFilters) {
                textBuilder.append("**").append(queryFilter.getName()).append("**")
                        .append(" ")
                        .append(queryFilter.getOperator().getValue())
                        .append(" ")
                        .append(queryFilter.getValue())
                        .append(" ");
            }
        }
        parseInfo.setTextInfo(textBuilder.toString());
    }

    private void processMultiTurn(ChatParseContext chatParseContext, ParseResp parseResp) {
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
        String histSQL = lastParseResult.getSelectedParses().get(0).getSqlInfo().getCorrectS2SQL();
        String rewrittenQuery = rewriteQuery(RewriteContext.builder()
                .curtQuestion(currentMapResult.getQueryText())
                .histQuestion(lastParseResult.getQueryText())
                .curtSchema(curtMapStr)
                .histSchema(histMapStr)
                .histSQL(histSQL)
                .llmConfig(queryReq.getLlmConfig())
                .build());
        chatParseContext.setQueryText(rewrittenQuery);
        log.info("Last Query: {} Current Query: {}, Rewritten Query: {}",
                lastParseResult.getQueryText(), currentMapResult.getQueryText(), rewrittenQuery);
    }

    private String rewriteQuery(RewriteContext context) {
        String promptStr = String.format(REWRITE_INSTRUCTION, context.getCurtQuestion(), context.getCurtSchema(),
                context.getHistQuestion(), context.getHistSchema(), context.getHistSQL());
        Prompt prompt = PromptTemplate.from(promptStr).apply(Collections.EMPTY_MAP);
        keyPipelineLog.info("NL2SQLParser reqPrompt:{}", promptStr);

        ChatLanguageModel chatLanguageModel = S2ChatModelProvider.provide(context.getLlmConfig());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());

        String result = response.content().text();
        keyPipelineLog.info("NL2SQLParser modelResp:{}", result);
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

    @Builder
    @Data
    public static class RewriteContext {

        private String curtQuestion;
        private String histQuestion;
        private String curtSchema;
        private String histSchema;
        private String histSQL;
        private LLMConfig llmConfig;
    }

}
