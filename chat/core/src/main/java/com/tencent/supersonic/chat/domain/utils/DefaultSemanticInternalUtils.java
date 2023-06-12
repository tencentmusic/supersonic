package com.tencent.supersonic.chat.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.LIST_LOWER;
import static com.tencent.supersonic.common.constant.Constants.PAGESIZE_LOWER;
import static com.tencent.supersonic.common.constant.Constants.TOTAL_LOWER;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.DomainResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.chat.application.ConfigServiceImpl;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.common.result.ResultData;
import com.tencent.supersonic.common.result.ReturnCode;
import com.tencent.supersonic.common.util.context.S2ThreadContext;
import com.tencent.supersonic.common.util.context.ThreadContext;
import com.tencent.supersonic.common.util.json.JsonUtil;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class DefaultSemanticInternalUtils {

    @Value("${semantic.url.prefix:http://localhost:8081}")
    private String semanticUrl;

    @Value("${fetchDomainList.path:/api/semantic/schema/dimension/page}")
    private String fetchDimensionPagePath;

    @Value("${fetchDomainList.path:/api/semantic/schema/metric/page}")
    private String fetchMetricPagePath;

    @Value("${fetchDomainList.path:/api/semantic/schema/domain/list}")
    private String fetchDomainListPath;


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfigServiceImpl chaConfigService;

    @Autowired
    private S2ThreadContext s2ThreadContext;

    @Autowired
    private AuthenticationConfig authenticationConfig;


    public ChatConfigRichInfo getChatConfigRichInfo(Long domain) {
        return chaConfigService.getConfigRichInfo(domain);
    }


    public PageInfo<MetricResp> queryMetricPage(PageMetricReq pageMetricCmd, User user) {
        String body = JsonUtil.toString(pageMetricCmd);
        Object dimensionListObject = fetchHttpResult(semanticUrl + fetchMetricPagePath, body, HttpMethod.POST);
        LinkedHashMap map = (LinkedHashMap) dimensionListObject;
        PageInfo<Object> metricDescObjectPageInfo = generatePageInfo(map);
        PageInfo<MetricResp> metricDescPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(metricDescObjectPageInfo, metricDescPageInfo);
        metricDescPageInfo.setList(metricDescPageInfo.getList());
        return metricDescPageInfo;
    }

    public PageInfo<DimensionResp> queryDimensionPage(PageDimensionReq pageDimensionCmd, User user) {
        String body = JsonUtil.toString(pageDimensionCmd);
        Object dimensionListObject = fetchHttpResult(semanticUrl + fetchDimensionPagePath, body, HttpMethod.POST);
        LinkedHashMap map = (LinkedHashMap) dimensionListObject;
        PageInfo<Object> dimensionDescObjectPageInfo = generatePageInfo(map);
        PageInfo<DimensionResp> dimensionDescPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(dimensionDescObjectPageInfo, dimensionDescPageInfo);
        dimensionDescPageInfo.setList(dimensionDescPageInfo.getList());
        return dimensionDescPageInfo;
    }

    private PageInfo<Object> generatePageInfo(LinkedHashMap map) {
        PageInfo<Object> pageInfo = new PageInfo<>();
        pageInfo.setList((List<Object>) map.get(LIST_LOWER));
        Integer total = (Integer) map.get(TOTAL_LOWER);
        pageInfo.setTotal(total);
        Integer pageSize = (Integer) map.get(PAGESIZE_LOWER);
        pageInfo.setPageSize(pageSize);
        pageInfo.setPages((int) Math.ceil((double) total / pageSize));
        return pageInfo;
    }


    public Object fetchHttpResult(String url, String bodyJson, HttpMethod httpMethod) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        fillToken(headers);
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(url).build().encode().toUri();
        ParameterizedTypeReference<ResultData<Object>> responseTypeRef =
                new ParameterizedTypeReference<ResultData<Object>>() {
                };
        HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toString(bodyJson), headers);
        try {
            ResponseEntity<ResultData<Object>> responseEntity = restTemplate.exchange(requestUrl,
                    httpMethod, entity, responseTypeRef);
            ResultData<Object> responseBody = responseEntity.getBody();
            log.debug("ApiResponse<fetchDomainSchema> responseBody:{}", responseBody);
            if (ReturnCode.SUCCESS.getCode() == responseBody.getCode()) {
                Object data = responseBody.getData();
                return data;
            }
        } catch (Exception e) {
            throw new RuntimeException("fetchDomainSchema interface error", e);
        }
        throw new RuntimeException("fetchDomainSchema interface error");
    }

    public void fillToken(HttpHeaders headers) {
        ThreadContext threadContext = s2ThreadContext.get();
        if (Objects.nonNull(threadContext) && Strings.isNotEmpty(threadContext.getToken())) {
            if (Objects.nonNull(authenticationConfig) && Strings.isNotEmpty(
                    authenticationConfig.getTokenHttpHeaderKey())) {
                headers.set(authenticationConfig.getTokenHttpHeaderKey(), threadContext.getToken());
            }
        } else {
            log.info("threadContext is null:{}", Objects.isNull(threadContext));
        }
    }

    public List<DomainResp> getDomainListForUser(User user) {
        Object domainDescListObject = fetchHttpResult(semanticUrl + fetchDomainListPath, null, HttpMethod.GET);
        List<DomainResp> domainDescList = (List<DomainResp>) domainDescListObject;
        return domainDescList;
    }
}