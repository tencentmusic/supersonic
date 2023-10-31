package com.tencent.supersonic.knowledge.semantic;

import static com.tencent.supersonic.common.pojo.Constants.LIST_LOWER;
import static com.tencent.supersonic.common.pojo.Constants.PAGESIZE_LOWER;
import static com.tencent.supersonic.common.pojo.Constants.TOTAL_LOWER;
import static com.tencent.supersonic.common.pojo.Constants.TRUE_LOWER;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.constant.UserConstants;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.common.pojo.ReturnCode;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.exception.CommonException;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.S2ThreadContext;
import com.tencent.supersonic.common.util.ThreadContext;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.ExplainSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryDimValueReq;
import com.tencent.supersonic.semantic.api.query.request.QueryS2QLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class RemoteSemanticInterpreter extends BaseSemanticInterpreter {

    private S2ThreadContext s2ThreadContext;

    private AuthenticationConfig authenticationConfig;

    private ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>> structTypeRef =
            new ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>>() {
            };

    private ParameterizedTypeReference<ResultData<ExplainResp>> explainTypeRef =
            new ParameterizedTypeReference<ResultData<ExplainResp>>() {
            };

    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructReq, User user) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return searchByRestTemplate(
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getSearchByStructPath(),
                new Gson().toJson(queryStructReq));
    }

    @Override
    public QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return searchByRestTemplate(
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getSearchByMultiStructPath(),
                new Gson().toJson(queryMultiStructReq));
    }

    @Override
    public QueryResultWithSchemaResp queryByS2QL(QueryS2QLReq queryS2QLReq, User user) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return searchByRestTemplate(defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getSearchBySqlPath(),
                new Gson().toJson(queryS2QLReq));
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

    @Override
    public QueryResultWithSchemaResp queryDimValue(QueryDimValueReq queryDimValueReq, User user) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return searchByRestTemplate(defaultSemanticConfig.getSemanticUrl()
                        + defaultSemanticConfig.getQueryDimValuePath(),
                new Gson().toJson(queryDimValueReq));
    }

    @Override
    public List<ModelSchemaResp> doFetchModelSchema(List<Long> ids) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(UserConstants.INTERNAL, TRUE_LOWER);
        headers.setContentType(MediaType.APPLICATION_JSON);
        fillToken(headers);
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);

        String semanticUrl = defaultSemanticConfig.getSemanticUrl();
        String fetchModelSchemaPath = defaultSemanticConfig.getFetchModelSchemaPath();
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(semanticUrl + fetchModelSchemaPath)
                .build().encode().toUri();
        ModelSchemaFilterReq filter = new ModelSchemaFilterReq();
        filter.setModelIds(ids);
        ParameterizedTypeReference<ResultData<List<ModelSchemaResp>>> responseTypeRef =
                new ParameterizedTypeReference<ResultData<List<ModelSchemaResp>>>() {
                };

        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(filter), headers);

        try {
            RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
            ResponseEntity<ResultData<List<ModelSchemaResp>>> responseEntity = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, responseTypeRef);
            ResultData<List<ModelSchemaResp>> responseBody = responseEntity.getBody();
            log.debug("ApiResponse<fetchModelSchema> responseBody:{}", responseBody);
            if (ReturnCode.SUCCESS.getCode() == responseBody.getCode()) {
                List<ModelSchemaResp> data = responseBody.getData();
                return data;
            }
        } catch (Exception e) {
            throw new RuntimeException("fetchModelSchema interface error", e);
        }
        throw new RuntimeException("fetchModelSchema interface error");
    }

    @Override
    public List<DomainResp> getDomainList(User user) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        Object domainDescListObject = fetchHttpResult(
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchDomainListPath(),
                null, HttpMethod.GET);
        return JsonUtil.toList(JsonUtil.toString(domainDescListObject), DomainResp.class);
    }

    @Override
    public List<ModelResp> getModelList(AuthType authType, Long domainId, User user) {
        if (domainId == null) {
            domainId = 0L;
        }
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        String url = String.format("%s?domainId=%s&authType=%s",
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchModelListPath(),
                domainId, authType.toString());
        Object domainDescListObject = fetchHttpResult(url, null, HttpMethod.GET);
        return JsonUtil.toList(JsonUtil.toString(domainDescListObject), ModelResp.class);
    }

    @Override
    public <T> ExplainResp explain(ExplainSqlReq<T> explainResp, User user) throws Exception {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        String semanticUrl = defaultSemanticConfig.getSemanticUrl();
        String explainPath = defaultSemanticConfig.getExplainPath();
        URL url = new URL(new URL(semanticUrl), explainPath);
        return explain(url.toString(), JsonUtil.toString(explainResp));
    }

    public ExplainResp explain(String url, String jsonReq) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        fillToken(headers);
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(url).build().encode().toUri();
        HttpEntity<String> entity = new HttpEntity<>(jsonReq, headers);
        log.info("url:{},explain:{}", url, entity.getBody());
        ResultData<ExplainResp> responseBody;
        try {
            RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);

            ResponseEntity<ResultData<ExplainResp>> responseEntity = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, explainTypeRef);
            log.info("ApiResponse<ExplainResp> responseBody:{}", responseEntity);
            responseBody = responseEntity.getBody();
            if (Objects.nonNull(responseBody.getData())) {
                return responseBody.getData();
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("explain interface error,url:" + url, e);
        }
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
            log.debug("ApiResponse<fetchModelSchema> responseBody:{}", responseBody);
            if (ReturnCode.SUCCESS.getCode() == responseBody.getCode()) {
                Object data = responseBody.getData();
                return data;
            }
        } catch (Exception e) {
            throw new RuntimeException("fetchModelSchema interface error", e);
        }
        throw new RuntimeException("fetchModelSchema interface error");
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
    public PageInfo<MetricResp> getMetricPage(PageMetricReq pageMetricCmd, User user) {
        String body = JsonUtil.toString(pageMetricCmd);
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        log.info("url:{}", defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchMetricPagePath());
        Object dimensionListObject = fetchHttpResult(
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchMetricPagePath(),
                body, HttpMethod.POST);
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
        Object dimensionListObject = fetchHttpResult(
                defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getFetchDimensionPagePath(),
                body, HttpMethod.POST);
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
