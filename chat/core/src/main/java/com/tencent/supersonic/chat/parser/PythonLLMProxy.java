package com.tencent.supersonic.chat.parser;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.chat.config.LLMParserConfig;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionCallConfig;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionReq;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionResp;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import java.net.URI;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * PythonLLMProxy sends requests to LangChain-based python service.
 */
@Slf4j
public class PythonLLMProxy implements LLMProxy {

    public LLMResp query2sql(LLMReq llmReq, String modelClusterKey) {

        long startTime = System.currentTimeMillis();
        log.info("requestLLM request, modelId:{},llmReq:{}", modelClusterKey, llmReq);
        try {
            LLMParserConfig llmParserConfig = ContextUtils.getBean(LLMParserConfig.class);

            URL url = new URL(new URL(llmParserConfig.getUrl()), llmParserConfig.getQueryToSqlPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toString(llmReq), headers);
            RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
            ResponseEntity<LLMResp> responseEntity = restTemplate.exchange(url.toString(), HttpMethod.POST, entity,
                    LLMResp.class);

            log.info("requestLLM response,cost:{}, questUrl:{} \n entity:{} \n body:{}",
                    System.currentTimeMillis() - startTime, url, entity, responseEntity.getBody());
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("requestLLM error", e);
        }
        return null;
    }

    public FunctionResp requestFunction(FunctionReq functionReq) {
        FunctionCallConfig functionCallInfoConfig = ContextUtils.getBean(FunctionCallConfig.class);
        String url = functionCallInfoConfig.getUrl() + functionCallInfoConfig.getPluginSelectPath();
        HttpHeaders headers = new HttpHeaders();
        long startTime = System.currentTimeMillis();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(functionReq), headers);
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(url).build().encode().toUri();
        RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
        try {
            log.info("requestFunction functionReq:{}", JsonUtil.toString(functionReq));
            ResponseEntity<FunctionResp> responseEntity = restTemplate.exchange(requestUrl, HttpMethod.POST, entity,
                    FunctionResp.class);
            log.info("requestFunction responseEntity:{},cost:{}", responseEntity,
                    System.currentTimeMillis() - startTime);
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("requestFunction error", e);
        }
        return null;
    }
}
