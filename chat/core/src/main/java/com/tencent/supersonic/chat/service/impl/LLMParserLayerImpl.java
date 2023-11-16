package com.tencent.supersonic.chat.service.impl;

import com.tencent.supersonic.chat.config.LLMParserConfig;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.chat.service.LLMParserLayer;
import com.tencent.supersonic.common.util.JsonUtil;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class LLMParserLayerImpl implements LLMParserLayer {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LLMParserConfig llmParserConfig;

    public LLMResp query2sql(LLMReq llmReq, Long modelId) {
        long startTime = System.currentTimeMillis();
        log.info("requestLLM request, modelId:{},llmReq:{}", modelId, llmReq);
        try {
            URL url = new URL(new URL(llmParserConfig.getUrl()), llmParserConfig.getQueryToSqlPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toString(llmReq), headers);
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
}
