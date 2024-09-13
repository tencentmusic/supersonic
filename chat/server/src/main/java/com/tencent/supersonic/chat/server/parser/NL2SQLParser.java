package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.server.agent.MultiTurnConfig;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ChatContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.service.ChatContextService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.service.impl.ExemplarServiceImpl;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.chat.server.parser.ParserConfig.PARSER_MULTI_TURN_ENABLE;

@Slf4j
public class NL2SQLParser implements ChatQueryParser {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    private static final String REWRITE_USER_QUESTION_INSTRUCTION =
            ""
                    + "#Role: You are a data product manager experienced in data requirements."
                    + "#Task: Your will be provided with current and history questions asked by a user,"
                    + "along with their mapped schema elements(metric, dimension and value),"
                    + "please try understanding the semantics and rewrite a question."
                    + "#Rules: "
                    + "1.ALWAYS keep relevant entities, metrics, dimensions, values and date ranges."
                    + "2.ONLY respond with the rewritten question."
                    + "#Current Question: {{current_question}}"
                    + "#Current Mapped Schema: {{current_schema}}"
                    + "#History Question: {{history_question}}"
                    + "#History Mapped Schema: {{history_schema}}"
                    + "#History SQL: {{history_sql}}"
                    + "#Rewritten Question: ";

    private static final String REWRITE_ERROR_MESSAGE_INSTRUCTION =
            ""
                    + "#Role: You are a data business partner who closely interacts with business people.\n"
                    + "#Task: Your will be provided with user input, system output and some examples, "
                    + "please respond shortly to teach user how to ask the right question, "
                    + "by using `Examples` as references."
                    + "#Rules: ALWAYS respond with the same language as the `Input`.\n"
                    + "#Input: {{user_question}}\n"
                    + "#Output: {{system_message}}\n"
                    + "#Examples: {{examples}}\n"
                    + "#Response: ";

    @Override
    public void parse(ParseContext parseContext, ParseResp parseResp) {
        if (!parseContext.enableNL2SQL() || checkSkip(parseResp)) {
            return;
        }
        ChatContextService chatContextService = ContextUtils.getBean(ChatContextService.class);
        ChatContext chatCtx = chatContextService.getOrCreateContext(parseContext.getChatId());

        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(parseContext.getAgent().getModelConfig());

        processMultiTurn(chatLanguageModel, parseContext);
        QueryNLReq queryNLReq = QueryReqConverter.buildText2SqlQueryReq(parseContext, chatCtx);
        addDynamicExemplars(parseContext.getAgent().getId(), queryNLReq);

        ChatLayerService chatLayerService = ContextUtils.getBean(ChatLayerService.class);
        ParseResp text2SqlParseResp = chatLayerService.performParsing(queryNLReq);
        if (ParseResp.ParseState.COMPLETED.equals(text2SqlParseResp.getState())) {
            parseResp.getSelectedParses().addAll(text2SqlParseResp.getSelectedParses());
        } else {
            parseResp.setErrorMsg(
                    rewriteErrorMessage(
                            chatLanguageModel,
                            parseContext.getQueryText(),
                            text2SqlParseResp.getErrorMsg(),
                            queryNLReq.getDynamicExemplars(),
                            parseContext.getAgent().getExamples()));
        }
        parseResp.setState(text2SqlParseResp.getState());
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
        metric.ifPresent(
                schemaElement ->
                        textBuilder.append("**指标:** ").append(schemaElement.getName()).append(" "));
        List<String> dimensionNames =
                parseInfo.getDimensions().stream()
                        .map(SchemaElement::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(dimensionNames)) {
            textBuilder.append("**维度:** ").append(String.join(",", dimensionNames));
        }
        textBuilder.append("\n\n**筛选条件:** \n");
        if (parseInfo.getDateInfo() != null) {
            textBuilder
                    .append("**数据时间:** ")
                    .append(parseInfo.getDateInfo().getStartDate())
                    .append("~")
                    .append(parseInfo.getDateInfo().getEndDate())
                    .append(" ");
        }
        if (!CollectionUtils.isEmpty(parseInfo.getDimensionFilters())
                || CollectionUtils.isEmpty(parseInfo.getMetricFilters())) {
            Set<QueryFilter> queryFilters = parseInfo.getDimensionFilters();
            queryFilters.addAll(parseInfo.getMetricFilters());
            for (QueryFilter queryFilter : queryFilters) {
                textBuilder
                        .append("**")
                        .append(queryFilter.getName())
                        .append("**")
                        .append(" ")
                        .append(queryFilter.getOperator().getValue())
                        .append(" ")
                        .append(queryFilter.getValue())
                        .append(" ");
            }
        }
        parseInfo.setTextInfo(textBuilder.toString());
    }

    private void processMultiTurn(ChatLanguageModel chatLanguageModel, ParseContext parseContext) {
        ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
        MultiTurnConfig agentMultiTurnConfig = parseContext.getAgent().getMultiTurnConfig();
        Boolean globalMultiTurnConfig =
                Boolean.valueOf(parserConfig.getParameterValue(PARSER_MULTI_TURN_ENABLE));

        Boolean multiTurnConfig =
                agentMultiTurnConfig != null
                        ? agentMultiTurnConfig.isEnableMultiTurn()
                        : globalMultiTurnConfig;
        if (!Boolean.TRUE.equals(multiTurnConfig)) {
            return;
        }

        // derive mapping result of current question and parsing result of last question.
        ChatLayerService chatLayerService = ContextUtils.getBean(ChatLayerService.class);
        QueryNLReq queryNLReq = QueryReqConverter.buildText2SqlQueryReq(parseContext);
        MapResp currentMapResult = chatLayerService.performMapping(queryNLReq);

        List<QueryResp> historyQueries = getHistoryQueries(parseContext.getChatId(), 1);
        if (historyQueries.size() == 0) {
            return;
        }
        QueryResp lastQuery = historyQueries.get(0);
        SemanticParseInfo lastParseInfo = lastQuery.getParseInfos().get(0);
        Long dataId = lastParseInfo.getDataSetId();

        String curtMapStr =
                generateSchemaPrompt(currentMapResult.getMapInfo().getMatchedElements(dataId));
        String histMapStr = generateSchemaPrompt(lastParseInfo.getElementMatches());
        String histSQL = lastParseInfo.getSqlInfo().getCorrectedS2SQL();

        Map<String, Object> variables = new HashMap<>();
        variables.put("current_question", currentMapResult.getQueryText());
        variables.put("current_schema", curtMapStr);
        variables.put("history_question", lastQuery.getQueryText());
        variables.put("history_schema", histMapStr);
        variables.put("history_sql", histSQL);

        Prompt prompt = PromptTemplate.from(REWRITE_USER_QUESTION_INSTRUCTION).apply(variables);
        keyPipelineLog.info("NL2SQLParser reqPrompt:{}", prompt.text());

        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
        String rewrittenQuery = response.content().text();
        keyPipelineLog.info("NL2SQLParser modelResp:{}", rewrittenQuery);
        parseContext.setQueryText(rewrittenQuery);
        QueryNLReq rewrittenQueryNLReq = QueryReqConverter.buildText2SqlQueryReq(parseContext);
        MapResp rewrittenQueryMapResult = chatLayerService.performMapping(rewrittenQueryNLReq);
        parseContext.setMapInfo(rewrittenQueryMapResult.getMapInfo());
        log.info(
                "Last Query: {} Current Query: {}, Rewritten Query: {}",
                lastQuery.getQueryText(),
                currentMapResult.getQueryText(),
                rewrittenQuery);
    }

    private String rewriteErrorMessage(
            ChatLanguageModel chatLanguageModel,
            String userQuestion,
            String errMsg,
            List<Text2SQLExemplar> similarExemplars,
            List<String> agentExamples) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_question", userQuestion);
        variables.put("system_message", errMsg);

        StringBuilder exampleStr = new StringBuilder();
        similarExemplars.forEach(
                e ->
                        exampleStr.append(
                                String.format(
                                        "<Question:{%s},Schema:{%s}> ",
                                        e.getQuestion(), e.getDbSchema())));
        agentExamples.forEach(e -> exampleStr.append(String.format("<Question:{%s}> ", e)));
        variables.put("examples", exampleStr);

        Prompt prompt = PromptTemplate.from(REWRITE_ERROR_MESSAGE_INSTRUCTION).apply(variables);
        keyPipelineLog.info("NL2SQLParser reqPrompt:{}", prompt.text());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());

        String rewrittenMsg = response.content().text();
        keyPipelineLog.info("NL2SQLParser modelResp:{}", rewrittenMsg);

        return rewrittenMsg;
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

    private List<QueryResp> getHistoryQueries(int chatId, int multiNum) {
        ChatManageService chatManageService = ContextUtils.getBean(ChatManageService.class);
        List<QueryResp> contextualParseInfoList =
                chatManageService.getChatQueries(chatId).stream()
                        .filter(
                                q ->
                                        Objects.nonNull(q.getQueryResult())
                                                && q.getQueryResult().getQueryState()
                                                        == QueryState.SUCCESS)
                        .collect(Collectors.toList());

        List<QueryResp> contextualList =
                contextualParseInfoList.subList(
                        0, Math.min(multiNum, contextualParseInfoList.size()));
        Collections.reverse(contextualList);
        return contextualList;
    }

    private void addDynamicExemplars(Integer agentId, QueryNLReq queryNLReq) {
        ExemplarServiceImpl exemplarManager = ContextUtils.getBean(ExemplarServiceImpl.class);
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        String memoryCollectionName = embeddingConfig.getMemoryCollectionName(agentId);
        List<Text2SQLExemplar> exemplars =
                exemplarManager.recallExemplars(memoryCollectionName, queryNLReq.getQueryText(), 5);
        queryNLReq.getDynamicExemplars().addAll(exemplars);
    }
}
