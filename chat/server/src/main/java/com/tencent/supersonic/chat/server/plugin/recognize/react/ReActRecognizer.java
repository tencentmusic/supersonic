package com.tencent.supersonic.chat.server.plugin.recognize.react;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.parser.ChatQueryParser;
import com.tencent.supersonic.chat.server.parser.NL2SQLParser;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginManager;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.plugin.build.react.ReActUtils;
import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.service.impl.ExemplarServiceImpl;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.server.utils.ModelConfigHelper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/** ReActRecognizer */
@Slf4j
public class ReActRecognizer extends PluginRecognizer {

    private String samplesPrompt = """
            #问题： {{question}}
            #参数名(arguments) {{params}}
            """;
    private LLMReAct llmReAct;
    private ExemplarServiceImpl exemplarManager;
    private EmbeddingConfig embeddingConfig;
    private NL2SQLParser nl2SQLParser = null;

    private void init() {
        if (llmReAct == null || exemplarManager == null || embeddingConfig == null
                || nl2SQLParser == null) {
            init2();
        }
    }

    private synchronized void init2() {
        if (nl2SQLParser == null) {
            List<ChatQueryParser> chatQueryParsers = ComponentFactory.getChatParsers();
            this.nl2SQLParser = (NL2SQLParser) chatQueryParsers.stream()
                    .filter(e -> e instanceof NL2SQLParser).findAny().orElse(null);
        }
        if (llmReAct == null) {
            llmReAct = ContextUtils.getBean(LLMReAct.class);
        }
        if (exemplarManager == null) {
            exemplarManager = ContextUtils.getBean(ExemplarServiceImpl.class);
        }
        if (embeddingConfig == null) {
            embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        }
    }

    @Override
    protected List<ChatPlugin> getPluginList(ParseContext parseContext) {
        init();
        List<ChatPlugin> plugins = super.getPluginList(parseContext);
        plugins = plugins.stream().filter(e -> e.getType().equals("REACT"))
                .collect(Collectors.toList());
        return plugins;
    }

    public boolean checkPreCondition(ParseContext parseContext) {
        init();
        List<ChatPlugin> plugins = getPluginList(parseContext);
        return !CollectionUtils.isEmpty(plugins);
    }

    private List<QueryResp> getHistoryQueries(int chatId, int multiNum) {
        ChatManageService chatManageService = ContextUtils.getBean(ChatManageService.class);
        List<QueryResp> contextualParseInfoList = chatManageService.getChatQueries(chatId).stream()
                .filter(q -> Objects.nonNull(q.getQueryResult())
                        && q.getQueryResult().getQueryState() == QueryState.SUCCESS)
                .collect(Collectors.toList());

        List<QueryResp> contextualList = contextualParseInfoList.subList(0,
                Math.min(multiNum, contextualParseInfoList.size()));
        Collections.reverse(contextualList);
        return contextualList;
    }

    private void rewriteMultiTurn(ParseContext parseContext, QueryNLReq queryNLReq) {
        ChatApp chatApp =
                parseContext.getAgent().getChatAppConfig().get(NL2SQLParser.APP_KEY_MULTI_TURN);
        if (Objects.isNull(chatApp) || !chatApp.isEnable()) {
            return;
        }
        if (!chatApp.getPrompt().contains("{{current_question}}")
                || !chatApp.getPrompt().contains("{{history}}")) {
            return;
        }
        // derive mapping result of current question and parsing result of last question.
        ChatLayerService chatLayerService = ContextUtils.getBean(ChatLayerService.class);

        List<QueryResp> historyQueries =
                getHistoryQueries(parseContext.getRequest().getChatId(), 3);
        if (historyQueries.isEmpty()) {
            return;
        }
        StringBuffer buff = new StringBuffer();
        int i = 1;
        for (QueryResp qr : historyQueries) {
            if (StringUtils.isNotBlank(qr.getQueryResult().getTextResult())) {
                String text = qr.getQueryResult().getTextResult();
                if (StringUtils.isNotBlank(qr.getQueryResult().getTextSummary())) {
                    if (text.length() > qr.getQueryResult().getTextSummary().length()) {
                        text = qr.getQueryResult().getTextSummary();
                    }
                }
                buff.append("\r\n##第").append(i).append("轮问答：\r\n");
                buff.append("#用户提问：\r\n").append(qr.getQueryText()).append("\r\n");
                buff.append("#系统回答：\r\n").append(text).append("\r\n");
            }
            i++;
        }

        QueryResp lastQuery = historyQueries.get(0);
        Map<String, Object> variables = new HashMap<>();
        variables.put("current_question", parseContext.getRequest().getQueryText());
        variables.put("history", buff.toString());

        Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variables);
        Date now = new Date();
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(ModelConfigHelper.getChatModelConfig(chatApp));
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
        String rewrittenQuery = response.content().text();
        System.out.println("重写耗时:" + (new Date().getTime() - now.getTime()) / 1000);

        parseContext.getRequest().setQueryText(rewrittenQuery);
        queryNLReq.setQueryText(rewrittenQuery);
        log.info("Last Query: {} Current Query: {}, Rewritten Query: {}", lastQuery.getQueryText(),
                parseContext.getRequest().getQueryText(), rewrittenQuery);
    }

    private List<String> getSamplesPrompts(List<Text2SQLExemplar> exemplar,
            JSONObject parseModeConfig) {
        List<String> samplesPrompts = new ArrayList<>();
        int sampleSize = 0;
        for (Text2SQLExemplar ep : exemplar) {
            Map<String, Object> param = new HashMap<>();
            param.put("question", ep.getQuestion());
            param.put("params", ep.getDbSchema());
            samplesPrompts.add(PromptTemplate.from(samplesPrompt).apply(param).text());
            sampleSize++;
            if (sampleSize > 5)
                break;
        }
        if (samplesPrompts.size() == 0) {
            Map<String, Object> param = new HashMap<>();
            JSONArray examples = parseModeConfig.getJSONArray("examples");
            if (examples.size() > 0) {
                param.put("question", examples.getString(0));
                param.put("params", "{}");
                samplesPrompts.add(PromptTemplate.from(samplesPrompt).apply(param).text());
            }
        }
        return samplesPrompts;
    }

    private ToolSpecification createTool(String toolName, String toolDescription,
            List<String> samplesPrompts, List<JSONObject> paramsConfig) {
        Map<String, JsonSchemaElement> params = new HashMap<>();
        List<String> required = new ArrayList<>();
        for (JSONObject pc : paramsConfig) {
            String paramKey =
                    pc.getString("value") != null ? pc.getString("value") : pc.getString("key");
            params.put(paramKey,
                    JsonStringSchema.builder().description(pc.getString("key")).build());
            if (pc.getBooleanValue("isRequired")) {
                required.add(pc.getString("value"));
            }
        }

        ToolSpecification tool = ToolSpecification.builder().name(toolName)
                .description(toolDescription + "\r\t" + String.join("\r\n", samplesPrompts))
                .parameters(JsonObjectSchema.builder().additionalProperties(true).properties(params)
                        .required(required).build())
                .build();
        return tool;
    }

    @Override
    public PluginRecallResult recallPlugin(ParseContext parseContext) {
        init();
        QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
        this.rewriteMultiTurn(parseContext, queryNLReq);
        String text = parseContext.getRequest().getQueryText();
        List<ChatPlugin> plugins = getPluginList(parseContext);

        List<ToolSpecification> toolList = new ArrayList<>();
        List<Text2SQLExemplar> exemplarAll = exemplarManager.recallExemplars(
                embeddingConfig.getMemoryCollectionName(parseContext.getAgent().getId()), text,
                150);
        for (ChatPlugin plugin : plugins) {
            JSONObject config = JSON.parseObject(plugin.getConfig());
            JSONObject parseModeConfig = JSON.parseObject(plugin.getParseModeConfig());
            List<Text2SQLExemplar> exemplar = exemplarAll.stream()
                    .filter(e -> e.getSql().equals("select plugin_" + plugin.getId()))
                    .collect(Collectors.toList());

            JSONArray paramOptions = config.getJSONArray("paramOptions");
            List<JSONObject> params = paramOptions.stream().map(e -> {
                return (JSONObject) e;
            }).filter(e -> !"FORWARD".equals(e.getString("paramType")))
                    .collect(Collectors.toList());
            List<String> samplesPrompts = getSamplesPrompts(exemplar, parseModeConfig);
            toolList.add(createTool(plugin.getName(), parseModeConfig.getString("description"),
                    samplesPrompts, params));
        }
        ChatApp chatApp = parseContext.getAgent().getChatAppConfig().get(LLMReAct.APP_KEY);
        if (chatApp != null) {
            LLMResp res = llmReAct.generateTool(chatApp.getChatModelId(), toolList, text);
            if (res != null) {
                // 添加当前用户
                JSONObject params = JSON.parseObject(res.getSchema());
                params.put("currentUser", parseContext.getRequest().getUser().getName());
                params.put("queryId", parseContext.getRequest().getQueryId());
                res.setSchema(JSON.toJSONString(params));
                ChatPlugin plugin =
                        plugins.stream().filter(e -> e.getName().equals(res.getSqlOutput()))
                                .findFirst().orElseGet(null);
                return PluginRecallResult.builder().plugin(plugin).dataSetIds(new HashSet<>())
                        .llmResp(res).score(2).distance(1).build(); // TODO score distance
            }
        }
        return null;
    }

    @Override
    public void buildQuery(ParseContext parseContext, ChatParseResp parseResp,
            PluginRecallResult pluginRecallResult) {
        Agent agentCopy = null;
        JSONObject config = JSON.parseObject(pluginRecallResult.getPlugin().getConfig());
        if ("Agent".equals(config.getString("url"))) {// 如果该问题需要其他智能体回答
            JSONObject parseModeConfig =
                    JSON.parseObject(pluginRecallResult.getPlugin().getParseModeConfig());
            String AgentName = parseModeConfig.getString("name");
            AgentService agentService = ContextUtils.getBean(AgentService.class);
            List<Agent> agents = agentService.getAgents().stream()
                    .filter(e -> e.getName().equals(AgentName)).collect(Collectors.toList());
            if (agents.size() > 0) {
                agentCopy = parseContext.getAgent();
                if (agentCopy.getId().equals(agents.get(0).getId())) {// 因为多轮对话的提示词模板问题，导致该情况不好用
                    ReActUtils.saveMemory(parseContext, pluginRecallResult);
                    return;
                } else {
                    parseContext.setAgent(agents.get(0));
                    if (nl2SQLParser.accept(parseContext)) {
                        nl2SQLParser.parse(parseContext);
                    }
                    pluginRecallResult.getLlmResp()
                            .setSideInfo(String.valueOf(agents.get(0).getId()));
                }
            }
        } else {
            ChatPlugin plugin = pluginRecallResult.getPlugin();
            QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
            SchemaMapInfo schemaMapInfo = new SchemaMapInfo();
            SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(-1L, plugin, parseContext,
                    schemaMapInfo, pluginRecallResult.getDistance());
            semanticParseInfo.setId(1);
            semanticParseInfo.setQueryMode(plugin.getType());
            semanticParseInfo.setScore(pluginRecallResult.getScore());
            parseResp.getSelectedParses().add(semanticParseInfo);

        }
        SemanticParseInfo spi = parseContext.getResponse().getSelectedParses()
                .get(parseContext.getResponse().getSelectedParses().size() - 1);
        Text2SQLExemplar exemplar =
                Text2SQLExemplar.builder().question(parseContext.getRequest().getQueryText())
                        .sideInfo(pluginRecallResult.getLlmResp().getSideInfo())
                        .dbSchema(pluginRecallResult.getLlmResp().getSchema())
                        .sql(pluginRecallResult.getLlmResp().getSqlOutput()).build();
        spi.getProperties().put(Text2SQLExemplar.PROPERTY_KEY2, exemplar);
    }


}
