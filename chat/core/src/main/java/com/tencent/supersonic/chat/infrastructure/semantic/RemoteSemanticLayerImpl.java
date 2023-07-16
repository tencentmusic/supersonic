package com.tencent.supersonic.chat.infrastructure.semantic;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.constant.UserConstants;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.application.ConfigServiceImpl;
import com.tencent.supersonic.chat.domain.pojo.config.*;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.common.util.context.S2ThreadContext;
import com.tencent.supersonic.common.util.context.ThreadContext;
import com.tencent.supersonic.common.util.json.JsonUtil;
import com.tencent.supersonic.semantic.api.core.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.core.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.*;
import com.tencent.supersonic.semantic.api.query.request.QuerySqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.exception.CommonException;
import com.tencent.supersonic.common.result.ResultData;
import com.tencent.supersonic.common.result.ReturnCode;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static com.tencent.supersonic.common.constant.Constants.*;
import static com.tencent.supersonic.common.constant.Constants.PAGESIZE_LOWER;

@Slf4j
public class RemoteSemanticLayerImpl implements SemanticLayer {


    private RestTemplate restTemplate;

    @Autowired
    private ConfigServiceImpl configService;

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
    public QueryResultWithSchemaResp queryBySql(QuerySqlReq querySqlReq, User user) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return searchByRestTemplate(defaultSemanticConfig.getSemanticUrl() + defaultSemanticConfig.getSearchBySqlPath(),
                new Gson().toJson(querySqlReq));
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
            DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
            ResponseEntity<ResultData<QueryResultWithSchemaResp>> responseEntity = defaultSemanticConfig.getRestTemplate()
                    .exchange(requestUrl,
                            HttpMethod.POST, entity, structTypeRef);
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

    public List<DomainSchemaResp> fetchDomainSchemaAll(List<Long> ids) {
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
            ResponseEntity<ResultData<List<DomainSchemaResp>>> responseEntity = defaultSemanticConfig.getRestTemplate()
                    .exchange(requestUrl,
                            HttpMethod.POST, entity, responseTypeRef);
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


    @SneakyThrows
    public List<DomainSchemaResp> fetchDomainSchema(List<Long> ids, Boolean cacheEnable) {
        if (cacheEnable) {
            return domainSchemaCache.get(String.valueOf(ids), () -> {
                List<DomainSchemaResp> data = fetchDomainSchemaAll(ids);
                fillEntityNameAndFilterBlackElement(data);
                return data;
            });
        }
        List<DomainSchemaResp> data = fetchDomainSchemaAll(ids);
        fillEntityNameAndFilterBlackElement(data);
        return data;
    }

    @Override
    public DomainSchemaResp getDomainSchemaInfo(Long domain, Boolean cacheEnable) {
        List<Long> ids = new ArrayList<>();
        ids.add(domain);
        List<DomainSchemaResp> domainSchemaResps = fetchDomainSchema(ids, cacheEnable);
        if (!CollectionUtils.isEmpty(domainSchemaResps)) {
            Optional<DomainSchemaResp> domainSchemaResp = domainSchemaResps.stream()
                    .filter(d -> d.getId().equals(domain)).findFirst();
            if (domainSchemaResp.isPresent()) {
                DomainSchemaResp domainSchema = domainSchemaResp.get();
                return domainSchema;
            }
        }
        return null;
    }

    @Override
    public List<DomainSchemaResp> getDomainSchemaInfo(List<Long> ids) {
        return fetchDomainSchema(ids, true);
    }

    public DomainSchemaResp fillEntityNameAndFilterBlackElement(DomainSchemaResp domainSchemaResp) {
        if (Objects.isNull(domainSchemaResp) || Objects.isNull(domainSchemaResp.getId())) {
            return domainSchemaResp;
        }
        ChatConfigResp chaConfigInfo = getConfigBaseInfo(domainSchemaResp.getId());
        // fill entity names
        fillEntityNamesInfo(domainSchemaResp, chaConfigInfo);

        // filter black element
        filterBlackDim(domainSchemaResp, chaConfigInfo);
        filterBlackMetric(domainSchemaResp, chaConfigInfo);

        return domainSchemaResp;
    }

    public void fillEntityNameAndFilterBlackElement(List<DomainSchemaResp> domainSchemaRespList) {
        if (!CollectionUtils.isEmpty(domainSchemaRespList)) {
            domainSchemaRespList.stream()
                    .forEach(domainSchemaResp -> fillEntityNameAndFilterBlackElement(domainSchemaResp));
        }
    }

    private void filterBlackMetric(DomainSchemaResp domainSchemaResp, ChatConfigResp chaConfigInfo) {

        ItemVisibility visibility = generateFinalVisibility(chaConfigInfo);
        if (Objects.nonNull(chaConfigInfo) && Objects.nonNull(visibility)
                && !CollectionUtils.isEmpty(visibility.getBlackMetricIdList())
                && !CollectionUtils.isEmpty(domainSchemaResp.getMetrics())) {
            List<MetricSchemaResp> metric4Chat = domainSchemaResp.getMetrics().stream()
                    .filter(metric -> !visibility.getBlackMetricIdList().contains(metric.getId()))
                    .collect(Collectors.toList());
            domainSchemaResp.setMetrics(metric4Chat);
        }
    }

    private ItemVisibility generateFinalVisibility(ChatConfigResp chatConfigInfo) {
        ItemVisibility visibility = new ItemVisibility();

        ChatAggConfig chatAggConfig = chatConfigInfo.getChatAggConfig();
        ChatDetailConfig chatDetailConfig = chatConfigInfo.getChatDetailConfig();

        // both black is exist
        if (Objects.nonNull(chatAggConfig) && Objects.nonNull(chatAggConfig.getVisibility())
                && Objects.nonNull(chatDetailConfig) && Objects.nonNull(chatDetailConfig.getVisibility())) {
            List<Long> blackDimIdList = new ArrayList<>();
            blackDimIdList.addAll(chatAggConfig.getVisibility().getBlackDimIdList());
            blackDimIdList.retainAll(chatDetailConfig.getVisibility().getBlackDimIdList());
            List<Long> blackMetricIdList = new ArrayList<>();

            blackMetricIdList.addAll(chatAggConfig.getVisibility().getBlackMetricIdList());
            blackMetricIdList.retainAll(chatDetailConfig.getVisibility().getBlackMetricIdList());

            visibility.setBlackDimIdList(blackDimIdList);
            visibility.setBlackMetricIdList(blackMetricIdList);
        }
        return visibility;
    }

    private void filterBlackDim(DomainSchemaResp domainSchemaResp, ChatConfigResp chatConfigInfo) {
        ItemVisibility visibility = generateFinalVisibility(chatConfigInfo);
        if (Objects.nonNull(chatConfigInfo) && Objects.nonNull(visibility)
                && !CollectionUtils.isEmpty(visibility.getBlackDimIdList())
                && !CollectionUtils.isEmpty(domainSchemaResp.getDimensions())) {
            List<DimSchemaResp> dim4Chat = domainSchemaResp.getDimensions().stream()
                    .filter(dim -> !visibility.getBlackDimIdList().contains(dim.getId()))
                    .collect(Collectors.toList());
            domainSchemaResp.setDimensions(dim4Chat);
        }
    }

    private void fillEntityNamesInfo(DomainSchemaResp domainSchemaResp, ChatConfigResp chatConfigInfo) {
        if (Objects.nonNull(chatConfigInfo) && Objects.nonNull(chatConfigInfo.getChatDetailConfig())
                && Objects.nonNull(chatConfigInfo.getChatDetailConfig().getEntity())
                && !CollectionUtils.isEmpty(chatConfigInfo.getChatDetailConfig().getEntity().getNames())) {
            domainSchemaResp.setEntityNames(chatConfigInfo.getChatDetailConfig().getEntity().getNames());
        }
    }

    private void deletionDuplicated(QueryStructReq queryStructReq) {
        if (!CollectionUtils.isEmpty(queryStructReq.getGroups()) && queryStructReq.getGroups().size() > 1) {
            Set<String> groups = new HashSet<>();
            groups.addAll(queryStructReq.getGroups());
            queryStructReq.getGroups().clear();
            queryStructReq.getGroups().addAll(groups);
        }
    }

    private void onlyQueryFirstMetric(QueryStructReq queryStructReq) {
        if (!CollectionUtils.isEmpty(queryStructReq.getAggregators()) && queryStructReq.getAggregators().size() > 1) {
            log.info("multi metric in aggregators:{} , only query first one", queryStructReq.getAggregators());
            queryStructReq.setAggregators(queryStructReq.getAggregators().subList(0, 1));
        }
    }

    public ChatConfigResp getConfigBaseInfo(Long domain) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return defaultSemanticConfig.getConfigService().fetchConfigByDomainId(domain);
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
            restTemplate = ContextUtils.getBean(RestTemplate.class);
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
    public PageInfo<MetricResp> queryMetricPage(PageMetricReq pageMetricCmd) {
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
    public PageInfo<DimensionResp> queryDimensionPage(PageDimensionReq pageDimensionCmd) {
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
