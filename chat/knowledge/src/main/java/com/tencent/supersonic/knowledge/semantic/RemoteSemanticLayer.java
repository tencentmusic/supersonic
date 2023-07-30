package com.tencent.supersonic.knowledge.semantic;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.constant.UserConstants;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.S2ThreadContext;
import com.tencent.supersonic.common.util.ThreadContext;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.*;
import com.tencent.supersonic.semantic.api.query.request.QueryDslReq;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.pojo.exception.CommonException;
import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.common.pojo.ReturnCode;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static com.tencent.supersonic.common.pojo.Constants.*;
import static com.tencent.supersonic.common.pojo.Constants.PAGESIZE_LOWER;

@Slf4j
public class RemoteSemanticLayer extends BaseSemanticLayer {
    @Autowired
    private S2ThreadContext s2ThreadContext;
    @Autowired
    private AuthenticationConfig authenticationConfig;

    private static final Cache<String, List<DomainSchemaResp>> domainSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();
    private ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>> structTypeRef =
            new ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>>() {
            };

    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructReq, User user) {
        deletionDuplicated(queryStructReq);
        onlyQueryFirstMetric(queryStructReq);
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return searchByRestTemplate(
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getSearchByStructPath(),
                new Gson().toJson(queryStructReq));
    }

    @Override
    public QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user) {
        for (QueryStructReq queryStructReq : queryMultiStructReq.getQueryStructReqs()) {
            deletionDuplicated(queryStructReq);
            onlyQueryFirstMetric(queryStructReq);
        }
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return searchByRestTemplate(
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getSearchByMultiStructPath(),
                new Gson().toJson(queryMultiStructReq));
    }

    @Override
    public QueryResultWithSchemaResp queryByDsl(QueryDslReq queryDslReq, User user) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return searchByRestTemplate(defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getSearchBySqlPath(),
                new Gson().toJson(queryDslReq));
    }

    public QueryResultWithSchemaResp searchByRestTemplate(String url, String jsonReq) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        fillToken(headers);
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(url).build().encode().toUri();
        HttpEntity<String> entity = new HttpEntity<>(jsonReq, headers);
        log.info("url:{},searchByRestTemplate:{}", url, entity.getBody());
        ResultData<QueryResultWithSchemaResp> responseBody;
        try {
            RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);

            ResponseEntity<ResultData<QueryResultWithSchemaResp>> responseEntity = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, structTypeRef);
            responseBody = responseEntity.getBody();
            log.info("ApiResponse<QueryResultWithColumns> responseBody:{}", responseBody);
            QueryResultWithSchemaResp semanticQuery = new QueryResultWithSchemaResp();
            if (ReturnCode.SUCCESS.getCode() == responseBody.getCode()) {
                QueryResultWithSchemaResp data = responseBody.getData();
                semanticQuery.setColumns(data.getColumns());
                semanticQuery.setResultList(data.getResultList());
                semanticQuery.setSql(data.getSql());
                semanticQuery.setQueryAuthorization(data.getQueryAuthorization());
                return semanticQuery;
            }
        } catch (Exception e) {
            throw new RuntimeException("search semantic interface error,url:" + url, e);
        }
        throw new CommonException(responseBody.getCode(), responseBody.getMsg());
    }

    public List<DomainSchemaResp> doFetchDomainSchema(List<Long> ids) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(UserConstants.INTERNAL, TRUE_LOWER);
        headers.setContentType(MediaType.APPLICATION_JSON);
        fillToken(headers);
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);

        URI requestUrl = UriComponentsBuilder.fromHttpUrl(
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchDomainSchemaPath()).build()
                .encode().toUri();
        DomainSchemaFilterReq filter = new DomainSchemaFilterReq();
        filter.setDomainIds(ids);
        ParameterizedTypeReference<ResultData<List<DomainSchemaResp>>> responseTypeRef =
                new ParameterizedTypeReference<ResultData<List<DomainSchemaResp>>>() {
                };

        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(filter), headers);

        try {
            RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
            ResponseEntity<ResultData<List<DomainSchemaResp>>> responseEntity = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, responseTypeRef);
            ResultData<List<DomainSchemaResp>> responseBody = responseEntity.getBody();
            log.debug("ApiResponse<fetchDomainSchema> responseBody:{}", responseBody);
            if (ReturnCode.SUCCESS.getCode() == responseBody.getCode()) {
                List<DomainSchemaResp> data = responseBody.getData();
                return data;
            }
        } catch (Exception e) {
            throw new RuntimeException("fetchDomainSchema interface error", e);
        }
        throw new RuntimeException("fetchDomainSchema interface error");
    }

    @Override
    public List<DomainResp> getDomainListForViewer() {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        Object domainDescListObject = fetchHttpResult(defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchDomainViewListPath(), null, HttpMethod.GET);
        List<DomainResp> domainDescList = JsonUtil.toList(JsonUtil.toString(domainDescListObject), DomainResp.class);
        return domainDescList;
    }

    @Override
    public List<DomainResp> getDomainListForAdmin() {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        Object domainDescListObject = fetchHttpResult(defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchDomainListPath(), null, HttpMethod.GET);
        List<DomainResp> domainDescList = JsonUtil.toList(JsonUtil.toString(domainDescListObject), DomainResp.class);
        return domainDescList;
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
            RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
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
        s2ThreadContext = ContextUtils.getBean(S2ThreadContext.class);
        authenticationConfig = ContextUtils.getBean(AuthenticationConfig.class);
        ThreadContext threadContext = s2ThreadContext.get();
        if (Objects.nonNull(threadContext) && Strings.isNotEmpty(threadContext.getToken())) {
            if (Objects.nonNull(authenticationConfig) && Strings.isNotEmpty(
                    authenticationConfig.getTokenHttpHeaderKey())) {
                headers.set(authenticationConfig.getTokenHttpHeaderKey(), threadContext.getToken());
            }
        } else {
            log.debug("threadContext is null:{}", Objects.isNull(threadContext));
        }
    }

    @Override
    public PageInfo<MetricResp> getMetricPage(PageMetricReq pageMetricCmd) {
        String body = JsonUtil.toString(pageMetricCmd);
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        log.info("url:{}", defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchMetricPagePath());
        Object dimensionListObject = fetchHttpResult(defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchMetricPagePath(), body, HttpMethod.POST);
        LinkedHashMap map = (LinkedHashMap) dimensionListObject;
        PageInfo<Object> metricDescObjectPageInfo = generatePageInfo(map);
        PageInfo<MetricResp> metricDescPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(metricDescObjectPageInfo, metricDescPageInfo);
        metricDescPageInfo.setList(metricDescPageInfo.getList());
        return metricDescPageInfo;
    }

    @Override
    public PageInfo<DimensionResp> getDimensionPage(PageDimensionReq pageDimensionCmd) {
        String body = JsonUtil.toString(pageDimensionCmd);
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        Object dimensionListObject = fetchHttpResult(defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchDimensionPagePath(), body, HttpMethod.POST);
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

}
