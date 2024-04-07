package com.tencent.supersonic.headless.core.chat.parser.llm;


import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMSqlResp;
import com.tencent.supersonic.headless.core.config.OptimizationConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MultiTurnSqlGeneration implements SqlGeneration, InitializingBean {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");
    private static final String EXAMPLE_JSON_FILE = "multi_turn_s2ql_examplar.json";
    private TypeReference<List<SqlExample>> valueTypeRef = new TypeReference<List<SqlExample>>() {
    };

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private SqlExamplarLoader sqlExampleLoader;

    @Autowired
    private OptimizationConfig optimizationConfig;

    @Autowired
    private SqlPromptGenerator sqlPromptGenerator;

    @Value("${multi.num:5}")
    private Integer contextualNum;

    public List<SqlExample> getSqlExamples() {
        try {
            ClassPathResource resource = new ClassPathResource(EXAMPLE_JSON_FILE);
            InputStream inputStream = resource.getInputStream();
            return JsonUtil.INSTANCE.getObjectMapper().readValue(inputStream, valueTypeRef);
        } catch (IOException e) {
            log.error("has an exception:{}", e.getMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public LLMResp generation(LLMReq llmReq, Long dataSetId) {

        keyPipelineLog.info("dataSetId:{},llmReq:{}", dataSetId, JsonUtil.toString(llmReq));

        List<SqlExample> sqlExamples = getSqlExamples();
        List<Map<String, String>> exampleList = new ArrayList<>();
        for (int i = 0; i < sqlExamples.size(); i++) {
            SqlExample sqlExample = sqlExamples.get(i);
            Map<String, String> metaDataMap = JsonUtil.toMap(JsonUtil.toString(sqlExample), String.class, String.class);
            exampleList.add(metaDataMap);
        }

        if (!CollectionUtils.isEmpty(llmReq.getContextualParseInfoList())) {
            List<SemanticParseInfo> contextualParseInfoList = llmReq.getContextualParseInfoList().stream()
                    .filter(o -> o.getDataSetId().equals(dataSetId) && LLMSqlQuery.QUERY_MODE.equals(o.getQueryMode()))
                            .collect(Collectors.toList());
            List<SemanticParseInfo> contextualList = contextualParseInfoList
                    .subList(0, Math.min(contextualNum, contextualParseInfoList.size()));
            Collections.reverse(contextualList);
            llmReq.setContextualParseInfoList(contextualList);
        }

        //2.generator linking and sql prompt by sqlExamples,and generate response.
        String promptStr = sqlPromptGenerator.generateMultiTurnLinkingAndSqlPrompt(llmReq, exampleList, dataSetId);

        Prompt prompt = PromptTemplate.from(JsonUtil.toString(promptStr)).apply(new HashMap<>());
        keyPipelineLog.info("multiTurn request prompt:{}", prompt.toSystemMessage());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toSystemMessage());
        String result = response.content().text();
        keyPipelineLog.info("multiTurn model response:{}", result);
        //3.format response.
        String schemaLinkStr = OutputFormat.getSchemaLinks(response.content().text());
        String sql = OutputFormat.getSql(response.content().text());
        log.info("multi turn sql result:{}", sql);
        keyPipelineLog.info("multi turn sql result:{}", sql);
        Map<String, LLMSqlResp> sqlRespMap = new HashMap<>();
        sqlRespMap.put(sql, LLMSqlResp.builder().sqlWeight(1D).fewShots(exampleList).build());
        keyPipelineLog.info("schemaLinkStr:{},sqlRespMap:{}", schemaLinkStr, sqlRespMap);

        LLMResp llmResp = new LLMResp();
        llmResp.setQuery(llmReq.getQueryText());
        llmResp.setSqlRespMap(sqlRespMap);
        return llmResp;
    }

    @Override
    public void afterPropertiesSet() {
        //SqlGenerationFactory.addSqlGenerationForFactory(LLMReq.SqlGenerationMode.MULTIPLE_ROUNDS, this);
    }
}
