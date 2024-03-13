package com.tencent.supersonic.headless.core.chat.parser;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.core.config.LLMParserConfig;
import com.tencent.supersonic.headless.core.chat.parser.llm.OutputFormat;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.ArrayList;

/**
 * PythonLLMProxy sends requests to LangChain-based python service.
 */
@Slf4j
@Component
public class PythonLLMProxy implements LLMProxy {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    @Override
    public boolean isSkip(QueryContext queryContext) {
        LLMParserConfig llmParserConfig = ContextUtils.getBean(LLMParserConfig.class);
        if (StringUtils.isEmpty(llmParserConfig.getUrl())) {
            log.warn("llmParserUrl is empty, skip :{}", PythonLLMProxy.class.getName());
            return true;
        }
        return false;
    }

    public LLMResp query2sql(LLMReq llmReq, Long dataSetId) {
        long startTime = System.currentTimeMillis();
        log.info("requestLLM request, dataSetId:{},llmReq:{}", dataSetId, llmReq);
        keyPipelineLog.info("dataSetId:{},llmReq:{}", dataSetId, llmReq);
        try {
            LLMParserConfig llmParserConfig = ContextUtils.getBean(LLMParserConfig.class);

            URL url = new URL(new URL(llmParserConfig.getUrl()), llmParserConfig.getQueryToSqlPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toString(llmReq), headers);
            RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
            ResponseEntity<LLMResp> responseEntity = restTemplate.exchange(url.toString(), HttpMethod.POST, entity,
                    LLMResp.class);

            LLMResp llmResp = responseEntity.getBody();
            log.info("requestLLM response,cost:{}, questUrl:{} \n entity:{} \n body:{}",
                    System.currentTimeMillis() - startTime, url, entity, llmResp);
            keyPipelineLog.info("LLMResp:{}", llmResp);

            if (MapUtils.isEmpty(llmResp.getSqlRespMap())) {
                llmResp.setSqlRespMap(OutputFormat.buildSqlRespMap(new ArrayList<>(), llmResp.getSqlWeight()));
            }
            return llmResp;
        } catch (Exception e) {
            log.error("requestLLM error", e);
        }
        return null;
    }

}
