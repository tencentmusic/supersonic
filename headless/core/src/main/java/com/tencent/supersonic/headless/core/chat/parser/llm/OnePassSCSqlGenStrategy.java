package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.tencent.supersonic.headless.core.config.ParserConfig.PARSER_EXEMPLAR_RECALL_NUMBER;
import static com.tencent.supersonic.headless.core.config.ParserConfig.PARSER_FEW_SHOT_NUMBER;
import static com.tencent.supersonic.headless.core.config.ParserConfig.PARSER_SELF_CONSISTENCY_NUMBER;


@Service
@Slf4j
public class OnePassSCSqlGenStrategy extends SqlGenStrategy {

    @Autowired
    private DifyServiceClient difyServiceClient;

    @Override
    public LLMResp generate(LLMReq llmReq) {
        //1.retriever sqlExamples and generate exampleListPool
        keyPipelineLog.info("OnePassSCSqlGenStrategy llmReq:{}", llmReq);

        int exemplarRecallNumber = Integer.valueOf(parserConfig.getParameterValue(PARSER_EXEMPLAR_RECALL_NUMBER));
        int fewShotNumber = Integer.valueOf(parserConfig.getParameterValue(PARSER_FEW_SHOT_NUMBER));
        int selfConsistencyNumber = Integer.valueOf(parserConfig.getParameterValue(PARSER_SELF_CONSISTENCY_NUMBER));

        List<Map<String, String>> sqlExamples = exemplarManager.recallExemplars(llmReq.getQueryText(),
                exemplarRecallNumber);
        List<List<Map<String, String>>> exampleListPool = promptGenerator.getExampleCombos(sqlExamples,
                fewShotNumber, selfConsistencyNumber);

        //2.generator linking and sql prompt by sqlExamples,and parallel generate response.
        List<String> linkingSqlPromptPool = promptGenerator.generatePromptPool(llmReq, exampleListPool, true);
        List<String> llmResults = new CopyOnWriteArrayList<>();
        linkingSqlPromptPool.parallelStream().forEach(linkingSqlPrompt -> {
                    String result = difyServiceClient.generate(PromptEnhancer.enhanceDDLInfo(llmReq,
                            linkingSqlPrompt)).getAnswer();
                    llmResults.add(difyServiceClient.parseSQLResult(result));
                    keyPipelineLog.info("OnePassSCSqlGenStrategy modelResp:{}", result);
                }
        );
        //3.format response.
        List<String> sqlList = llmResults.stream()
                .map(OutputFormat::getSql).collect(Collectors.toList());

        Pair<String, Map<String, Double>> sqlMapPair = OutputFormat.selfConsistencyVote(sqlList);

        LLMResp result = new LLMResp();
        result.setQuery(llmReq.getQueryText());
        result.setSqlRespMap(OutputFormat.buildSqlRespMap(sqlExamples, sqlMapPair.getRight()));
        return result;
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenStrategyFactory.addSqlGenerationForFactory(LLMReq.SqlGenType.ONE_PASS_AUTO_COT_SELF_CONSISTENCY, this);
    }
}
