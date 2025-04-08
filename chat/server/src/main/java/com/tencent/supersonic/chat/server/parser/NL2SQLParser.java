package com.tencent.supersonic.chat.server.parser;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.service.impl.ExemplarServiceImpl;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.chat.service.RecommendedQuestionsService;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.server.utils.ModelConfigHelper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.chat.parser.ParserConfig.*;

@Slf4j
public class NL2SQLParser implements ChatQueryParser {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    public static final String APP_KEY_MULTI_TURN = "REWRITE_MULTI_TURN";
    private static final String REWRITE_MULTI_TURN_INSTRUCTION = ""
            + "#Role: You are a data product manager experienced in data requirements."
            + "#Task: Your will be provided with current and history questions asked by a user,"
            + "along with their mapped schema elements(metric, dimension and value),"
            + "please try understanding the semantics and rewrite a question." + "#Rules: "
            + "1.ALWAYS keep relevant entities, metrics, dimensions, values and date ranges."
            + "2.ONLY respond with the rewritten question."
            + "#Current Question: {{current_question}}"
            + "#Current Mapped Schema: {{current_schema}}"
            + "#History Question: {{history_question}}"
            + "#History Mapped Schema: {{history_schema}}" + "#History SQL: {{history_sql}}"
            + "#Rewritten Question: ";

    public NL2SQLParser() {
        ChatAppManager.register(APP_KEY_MULTI_TURN,
                ChatApp.builder().prompt(REWRITE_MULTI_TURN_INSTRUCTION).name("多轮对话改写")
                        .appModule(AppModule.CHAT).description("通过大模型根据历史对话来改写本轮对话").enable(false)
                        .build());
    }

    public boolean accept(ParseContext parseContext) {
        return parseContext.enableNL2SQL();
    }

    @Override
    public void parse(ParseContext parseContext) {
        // first go with rule-based parsers unless the user has already selected one parse.
        if (Objects.isNull(parseContext.getRequest().getSelectedParse())) {
            QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
            queryNLReq.setText2SQLType(Text2SQLType.ONLY_RULE);
            if (parseContext.enableLLM()) {
                queryNLReq.setText2SQLType(Text2SQLType.NONE);
            }

            // for every requested dataSet, recursively invoke rule-based parser with different
            // mapModes
            Set<Long> requestedDatasets = queryNLReq.getDataSetIds();
            List<SemanticParseInfo> candidateParses = Lists.newArrayList();
            StringBuilder errMsg = new StringBuilder();
            for (Long datasetId : requestedDatasets) {
                queryNLReq.setDataSetIds(Collections.singleton(datasetId));
                ChatParseResp parseResp = new ChatParseResp(parseContext.getRequest().getQueryId());
                for (MapModeEnum mode : Lists.newArrayList(MapModeEnum.STRICT,
                        MapModeEnum.MODERATE)) {
                    queryNLReq.setMapModeEnum(mode);
                    doParse(queryNLReq, parseResp);
                }
                Integer valueSize = 0;
                if (!parseResp.getSelectedParses().isEmpty()) {
                    valueSize = parseResp.getSelectedParses().get(0).getElementMatches().stream()
                            .filter(schemaElementMatch -> schemaElementMatch.getElement().getType() == SchemaElementType.VALUE)
                            .collect(Collectors.toList()).size();
                }
                if ((parseResp.getSelectedParses().isEmpty() && candidateParses.isEmpty())
                        || valueSize == 0) {
                    queryNLReq.setMapModeEnum(MapModeEnum.LOOSE);
                    doParse(queryNLReq, parseResp);

                }

                if (parseResp.getSelectedParses().isEmpty()) {
                    errMsg.append(parseResp.getErrorMsg());
                    continue;
                }
                // for one dataset select the top 1 parse after sorting
                SemanticParseInfo.sort(parseResp.getSelectedParses());
                candidateParses.add(parseResp.getSelectedParses().get(0));
            }
            ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
            int parserShowCount =
                    Integer.parseInt(parserConfig.getParameterValue(PARSER_SHOW_COUNT));
            SemanticParseInfo.sort(candidateParses);
            parseContext.getResponse().setSelectedParses(
                    candidateParses.subList(0, Math.min(parserShowCount, candidateParses.size())));
            if (parseContext.getResponse().getSelectedParses().isEmpty()) {
                parseContext.getResponse().setState(ParseResp.ParseState.FAILED);
                parseContext.getResponse().setErrorMsg(errMsg.toString());
            }
        }

        // next go with llm-based parsers unless LLM is disabled or use feedback is needed.
        if (parseContext.needLLMParse() && !parseContext.needFeedback()) {
            // either the user or the system selects one parse from the candidate parses.
            if (Objects.isNull(parseContext.getRequest().getSelectedParse())
                    && parseContext.getResponse().getSelectedParses().isEmpty()) {
                return;
            }

            QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
            queryNLReq.setText2SQLType(Text2SQLType.LLM_OR_RULE);
            SemanticParseInfo userSelectParse = parseContext.getRequest().getSelectedParse();
            queryNLReq.setSelectedParseInfo(Objects.nonNull(userSelectParse) ? userSelectParse
                    : parseContext.getResponse().getSelectedParses().get(0));
            parseContext.setResponse(new ChatParseResp(parseContext.getResponse().getQueryId()));
            // 1.多轮对话改写
            rewriteMultiTurn(parseContext, queryNLReq);
            // 2.fowShot召回，RAG向量库中召回
            addDynamicExemplars(parseContext, queryNLReq);
            // 3.调用Llm生成语义sql
            doParse(queryNLReq, parseContext.getResponse());

            // try again with all semantic fields passed to LLM
            if (parseContext.getResponse().getState().equals(ParseResp.ParseState.FAILED)) {
                queryNLReq.setSelectedParseInfo(null);
                queryNLReq.setMapModeEnum(MapModeEnum.ALL);
                doParse(queryNLReq, parseContext.getResponse());
            }
        }
    }

    private void doParse(QueryNLReq req, ChatParseResp resp) {
        ChatLayerService chatLayerService = ContextUtils.getBean(ChatLayerService.class);
        ParseResp parseResp = chatLayerService.parse(req);
        if (parseResp.getState().equals(ParseResp.ParseState.COMPLETED)) {
            resp.getSelectedParses().addAll(parseResp.getSelectedParses());
        }
        resp.setState(parseResp.getState());
        resp.setParseTimeCost(parseResp.getParseTimeCost());
        resp.setErrorMsg(parseResp.getErrorMsg());
    }

    public void rewriteMultiTurn(ParseContext parseContext, QueryNLReq queryNLReq) {
        ChatApp chatApp = parseContext.getAgent().getChatAppConfig().get(APP_KEY_MULTI_TURN);
        RecommendedQuestionsService recommendedQuestionsService =
                ContextUtils.getBean(RecommendedQuestionsService.class);
        boolean isRecommendQuestion = recommendedQuestionsService.findQuerySqlByQuestion(
                Math.toIntExact(queryNLReq.getAgentId()), queryNLReq.getQueryText()) != null;
        if (Objects.isNull(chatApp) || !chatApp.isEnable() || isRecommendQuestion) {
            return;
        }

        // derive mapping result of current question and parsing result of last question.
        ChatLayerService chatLayerService = ContextUtils.getBean(ChatLayerService.class);
        MapResp currentMapResult = chatLayerService.map(queryNLReq);

        List<QueryResp> historyQueries =
                getHistoryQueries(parseContext.getRequest().getUser().getName(),
                        parseContext.getRequest().getChatId());
        if (historyQueries.isEmpty()) {
            return;
        }
        Long dataId = queryNLReq.getDataSetIds().stream().findFirst().get();
        String curtMapStr =
                generateSchemaPrompt(currentMapResult.getMapInfo().getMatchedElements(dataId));
        List<HistoryQuery> historyQueryList = new ArrayList<>();
        historyQueries.forEach(hq -> {
            SemanticParseInfo lastParseInfo = hq.getParseInfos().get(0);
            String histMapStr = generateSchemaPrompt(lastParseInfo.getElementMatches());
            String histSQL = lastParseInfo.getSqlInfo().getCorrectedS2SQL();
            String histQuestion = hq.getQueryText();
            NL2SQLParser.HistoryQuery historyQuery =
                    new HistoryQuery(histSQL, histQuestion, histMapStr);
            historyQueryList.add(historyQuery);
        });
        String history_question = historyQueryList.stream().map(historyQuery -> {
            return historyQuery.getQuestion();
        }).collect(Collectors.joining("||"));
        String history_schema = historyQueryList.stream().map(historyQuery -> {
            return historyQuery.getSchema();
        }).collect(Collectors.joining("||"));
        String history_sql = historyQueryList.stream().map(historyQuery -> {
            return historyQuery.getSql();
        }).collect(Collectors.joining("||"));

        Map<String, Object> variables = new HashMap<>();
        variables.put("current_question", currentMapResult.getQueryText());
        variables.put("current_schema", curtMapStr);
        variables.put("history_question", history_question);
        variables.put("history_schema", history_schema);
        variables.put("history_sql", history_sql);

        Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variables);
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(ModelConfigHelper.getChatModelConfig(chatApp));
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
        String rewrittenQuery = response.content().text();
        keyPipelineLog.info("QueryRewrite modelReq:\n{} \nmodelResp:\n{}", prompt.text(), response);
        parseContext.getRequest().setQueryText(rewrittenQuery);
        queryNLReq.setQueryText(rewrittenQuery);
        log.info("Last Querys: {} Current Query: {}, Rewritten Query: {}", history_question,
                currentMapResult.getQueryText(), rewrittenQuery);
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

    private List<QueryResp> getHistoryQueries(String userName, int chatId) {
        ChatManageService chatManageService = ContextUtils.getBean(ChatManageService.class);
        ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
        List<QueryResp> contextualParseInfoList = chatManageService.getChatQueries(chatId).stream()
                .filter(q -> Objects.nonNull(q.getQueryResult())
                        && q.getQueryResult().getQueryState() == QueryState.SUCCESS
                        && StringUtils.endsWithIgnoreCase(userName, q.getUserName())
                        && DateUtils.calculateDiffMs(q.getCreateTime()) < 1000l * Integer
                        .parseInt(parserConfig.getParameterValue(CHAT_HISTORY_SECONDS)))
                .collect(Collectors.toList());

        List<QueryResp> contextualList = contextualParseInfoList.subList(0,
                Math.min(Integer.parseInt(parserConfig.getParameterValue(CHAT_HISTORY_NUM)),
                        contextualParseInfoList.size()));
        Collections.reverse(contextualList);
        return contextualList;
    }

    private void addDynamicExemplars(ParseContext parseContext, QueryNLReq queryNLReq) {
        ExemplarServiceImpl exemplarManager = ContextUtils.getBean(ExemplarServiceImpl.class);
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        String memoryCollectionName =
                embeddingConfig.getMemoryCollectionName(parseContext.getAgent().getId());
        ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
        int exemplarRecallNumber =
                Integer.parseInt(parserConfig.getParameterValue(PARSER_EXEMPLAR_RECALL_NUMBER));
        List<Text2SQLExemplar> exemplars = exemplarManager.recallExemplars(memoryCollectionName,
                queryNLReq.getQueryText(), exemplarRecallNumber);
        queryNLReq.getDynamicExemplars().addAll(exemplars);
        parseContext.getResponse().setUsedExemplars(exemplars);
    }

    public static class HistoryQuery {
        String sql;
        String question;
        String schema;

        public HistoryQuery(String sql, String question, String schema) {
            this.sql = sql;
            this.question = question;
            this.schema = schema;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }
    }
}
