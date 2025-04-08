package com.tencent.supersonic.headless.chat.parser.llm;

import com.amazonaws.services.bedrockagent.model.Agent;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.chat.service.RecommendedQuestionsService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class OnePassSCSqlGenStrategy extends SqlGenStrategy {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");
    public static final String APP_KEY = "S2SQL_PARSER";
    public static final String INSTRUCTION =
            "#Role: You are a data analyst experienced in SQL languages."
                    + "\n#Task: You will be provided with a natural language question asked by users,"
                    + "please convert it to a SQL query so that relevant data could be returned "
                    + "by executing the SQL query against underlying database." + "\n#Rules:"
                    + "\n1.SQL columns and values must be mentioned in the `Schema`, DO NOT hallucinate."
                    + "\n2.ALWAYS specify time range using `>`,`<`,`>=`,`<=` operator."
                    + "\n3.DO NOT include time range in the where clause if not explicitly expressed in the `Question`."
                    + "\n4.DO NOT calculate date range using functions."
                    + "\n5.ALWAYS use `with` statement if nested aggregation is needed."
                    + "\n6.ALWAYS enclose alias declared by `AS` command in underscores."
                    + "\n7.Alias created by `AS` command must be in the same language ast the `Question`."
                    + "\n#Exemplars: {{exemplar}}"
                    + "\n#Query: Question:{{question}},Schema:{{schema}},SideInfo:{{information}}";

    public OnePassSCSqlGenStrategy() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("语义SQL解析")
                .appModule(AppModule.CHAT).description("通过大模型做语义解析生成S2SQL").enable(true).build());
    }

    @Autowired
    private RecommendedQuestionsService recommendedQuestionsService;

    @Autowired
    @Qualifier("chatExecutor")
    private ThreadPoolExecutor executor;

    @Data
    static class SemanticSql {
        @Description("告诉用户有关这个SQL的查询思路，结合表的元数据与查询的条件数据, make it short.")
        private String thought;

        @Description("sql to generate")
        private String sql;

        @Description("如果问题与提供的上下文无关，请礼貌引导用户提问与当前表及数据的相关问题")
        private String message;
    }

    interface SemanticSqlExtractor {
        SemanticSql generateSemanticSql(String text);
    }

    interface StreamingSemanticParseExtractor {
        @SystemMessage("您的名字叫红海ChatBI, 您的职责是基于上下文给出查询思路.")
        @UserMessage("仅展示查询思路，不要出现表字段, 80-100字左右. {{it}}")
        Flux<String> generateStreamingSemanticParse(String text);
    }

    @Override
    public LLMResp generate(LLMReq llmReq) {

        // =================== 新增逻辑1：检查是否是推荐问题 ===================
        LLMResp recommendedResp = handleRecommendedQuestion(llmReq);
        if (recommendedResp != null) {
            log.info("匹配到推荐问题:\n{}", llmReq.getQueryText());
            return recommendedResp;
        }
        // =================== 新增逻辑2：检查是否是简易模型(直连模式) ===================
        if (isDirectLinkMode(llmReq)) {
            return handleDirectLinkMode(llmReq);
        }
        // ================== 原逻辑 ===================
        LLMResp llmResp = new LLMResp();
        llmResp.setQuery(llmReq.getQueryText());
        // 1.recall exemplars
        log.debug("OnePassSCSqlGenStrategy llmReq:\n{}", llmReq);
        List<List<Text2SQLExemplar>> exemplarsList = promptHelper.getFewShotExemplars(llmReq);

        // 2.generate sql generation prompt for each self-consistency inference
        ChatApp chatApp = llmReq.getChatAppConfig().get(APP_KEY);
        ChatLanguageModel chatLanguageModel = getChatLanguageModel(chatApp.getChatModelConfig());
        SemanticSqlExtractor extractor =
                AiServices.create(SemanticSqlExtractor.class, chatLanguageModel);

        Map<Prompt, List<Text2SQLExemplar>> prompt2Exemplar = new HashMap<>();
        for (List<Text2SQLExemplar> exemplars : exemplarsList) {
            llmReq.setDynamicExemplars(exemplars);
            Prompt prompt = generatePrompt(llmReq, llmResp, chatApp);
            prompt2Exemplar.put(prompt, exemplars);
        }

        // 3.perform multiple self-consistency inferences parallelly
        Map<String, Prompt> output2Prompt = new ConcurrentHashMap<>();
        prompt2Exemplar.keySet().parallelStream().forEach(prompt -> {
            SemanticSql s2Sql = extractor.generateSemanticSql(prompt.toUserMessage().singleText());
            output2Prompt.put(s2Sql.getSql(), prompt);
            keyPipelineLog.info("OnePassSCSqlGenStrategy modelReq:\n{} \nmodelResp:\n{}",
                    prompt.text(), s2Sql);
        });

        // 4.format response.
        Pair<String, Map<String, Double>> sqlMapPair =
                ResponseHelper.selfConsistencyVote(Lists.newArrayList(output2Prompt.keySet()));
        llmResp.setSqlOutput(sqlMapPair.getLeft());
        List<Text2SQLExemplar> usedExemplars =
                prompt2Exemplar.get(output2Prompt.get(sqlMapPair.getLeft()));
        llmResp.setSqlRespMap(ResponseHelper.buildSqlRespMap(usedExemplars, sqlMapPair.getRight()));
        return llmResp;
    }

    public SseEmitter streamGenerate(LLMReq llmReq) {
        // 1. 创建SSE发射器（1分钟超时）
        SseEmitter emitter = new SseEmitter(60_000L);
        // 2. 在异步线程中执行后续逻辑，线程池资源隔离
        CompletableFuture.runAsync(() -> {
            try {
                // 初始化响应对象
                LLMResp llmResp = new LLMResp();
                llmResp.setQuery(llmReq.getQueryText());
                // 获取模型配置
                ChatApp chatApp = llmReq.getChatAppConfig().get(APP_KEY);
                // 使用流式专用模型配置
                StreamingChatLanguageModel streamChatModel =
                        getStreamChatModel(chatApp.getChatModelConfig());
                // 创建流式解析器
                StreamingSemanticParseExtractor extractor =
                        AiServices.create(StreamingSemanticParseExtractor.class, streamChatModel);
                // 生成prompt
                SimpleStrategy simpleStrategy = new SimpleStrategy();
                Prompt promptText = simpleStrategy.generateStreamPrompt(llmReq);

                // 获取响应流，设置背压为最大100个元素
                Flux<String> thought = extractor
                        .generateStreamingSemanticParse(promptText.toUserMessage().singleText())
                        .onBackpressureBuffer(100);
                // 订阅响应流，设置延迟为100毫秒，并行调度
                Disposable subscription = thought.subscribe(chunk -> {
                    try {
                        // 发送单个数据块
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        log.error("SSE send error", e);
                        emitter.completeWithError(e);
                    }
                }, error -> {
                    log.error("Stream processing error", error);
                    emitter.completeWithError(error);
                }, () -> {
                    log.info("Stream completed successfully");
                    emitter.complete();
                });
                // 添加取消订阅处理
                emitter.onCompletion(subscription::dispose);
                emitter.onTimeout(() -> {
                    subscription.dispose();
                    emitter.complete();
                });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, executor);
        return emitter;
    }

    private boolean isDirectLinkMode(LLMReq llmReq) {
        return StringUtils.endsWithIgnoreCase(llmReq.getSchema().getDataSetName(), "直连模式");
    }

    private LLMResp handleDirectLinkMode(LLMReq llmReq) {
        LLMResp llmResp = new LLMResp();
        llmResp.setQuery(llmReq.getQueryText());
        Map<Prompt, List<Text2SQLExemplar>> prompt2Exemplar = new HashMap<>();
        try {
            List<List<Text2SQLExemplar>> exemplarsList =
                    promptHelper.getFewShotExemplarsByHistory(llmReq);
            for (List<Text2SQLExemplar> exemplars : exemplarsList) {
                llmReq.setDynamicExemplars(exemplars);
                SimpleStrategy simpleStrategy = new SimpleStrategy();
                Prompt promptText = simpleStrategy.generatePrompt(llmReq, promptHelper);
                prompt2Exemplar.put(promptText, exemplars);
            }
        } catch (Exception e) {
            log.warn("few-shot召唤异常", e);
        }


        ChatApp chatApp = llmReq.getChatAppConfig().get(APP_KEY);
        ChatLanguageModel languageModel = getChatLanguageModel(chatApp.getChatModelConfig());
        SemanticSqlExtractor extractor =
                AiServices.create(SemanticSqlExtractor.class, languageModel);

        Map<String, Prompt> output2Prompt = new ConcurrentHashMap<>();
        prompt2Exemplar.keySet().parallelStream().forEach(prompt -> {
            SemanticSql s2Sql = extractor.generateSemanticSql(prompt.toUserMessage().singleText());
            String key = pickFirstNonBlank(s2Sql.getSql(), s2Sql.getMessage(), s2Sql.getThought());
            output2Prompt.put(key, prompt);
            keyPipelineLog.info("OnePassSCSqlGenStrategy modelReq:\n{} \nmodelResp:\n{}",
                    prompt.text(), s2Sql);
        });

        Pair<String, Map<String, Double>> sqlMapPair =
                ResponseHelper.selfConsistencyVote(Lists.newArrayList(output2Prompt.keySet()));
        llmResp.setSqlOutput(sqlMapPair.getLeft());

        List<Text2SQLExemplar> usedExemplars =
                prompt2Exemplar.get(output2Prompt.get(sqlMapPair.getLeft()));
        llmResp.setSqlRespMap(ResponseHelper.buildSqlRespMap(usedExemplars, sqlMapPair.getRight()));

        log.info("Simplified model SQL generation, SQL: {}", llmResp.getSqlOutput());
        return llmResp;
    }

    /**
     * 返回第一个非空的字符串(先看 sql, 再看 message, 再看 thought)
     */
    private String pickFirstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (StringUtils.isNotBlank(c)) {
                return c;
            }
        }
        return "";
    }

    /**
     * 如果当前提问是“推荐问题”，则直接返回包含该 SQL 的 LLMResp； 若不是，返回 null。
     */
    private LLMResp handleRecommendedQuestion(LLMReq llmReq) {
        if (llmReq.getAgentId() == null) {
            return null;
        }
        // 去掉用户输入问题末尾的符号
        String queryText = llmReq.getQueryText().replaceAll("[。？！.,?！]+$", "");
        String querySql = recommendedQuestionsService
                .findQuerySqlByQuestion(Math.toIntExact(llmReq.getAgentId()), queryText);
        if (StringUtils.isNotEmpty(querySql)) {
            LLMResp resp = new LLMResp();
            resp.setQuery(llmReq.getQueryText());
            resp.setSqlOutput(querySql);
            resp.setSqlRespMap(ResponseHelper.buildSqlRespMap(Collections.emptyList(),
                    Collections.emptyMap()));
            log.info("查到推荐问题对应的sql: {}", querySql);
            return resp;
        }
        return null;
    }

    private Prompt generatePrompt(LLMReq llmReq, LLMResp llmResp, ChatApp chatApp) {
        StringBuilder exemplars = new StringBuilder();
        for (Text2SQLExemplar exemplar : llmReq.getDynamicExemplars()) {
            String exemplarStr = String.format("\nQuestion:%s,Schema:%s,SideInfo:%s,SQL:%s",
                    exemplar.getQuestion(), exemplar.getDbSchema(), exemplar.getSideInfo(),
                    exemplar.getSql());
            exemplars.append(exemplarStr);
        }
        String dataSemantics = promptHelper.buildSchemaStr(llmReq);
        String sideInformation = promptHelper.buildSideInformation(llmReq);
        llmResp.setSchema(dataSemantics);
        llmResp.setSideInfo(sideInformation);

        Map<String, Object> variable = new HashMap<>();
        variable.put("exemplar", exemplars);
        variable.put("question", llmReq.getQueryText());
        variable.put("schema", dataSemantics);
        variable.put("information", sideInformation);

        // use custom prompt template if provided.
        String promptTemplate = chatApp.getPrompt();
        return PromptTemplate.from(promptTemplate).apply(variable);
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenStrategyFactory
                .addSqlGenerationForFactory(LLMReq.SqlGenType.ONE_PASS_SELF_CONSISTENCY, this);
    }

}
