package com.tencent.supersonic.chat.infrastructure.semantic;

import static com.tencent.supersonic.common.constant.Constants.TRUE_LOWER;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authentication.constant.UserConstants;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.api.core.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QuerySqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ItemVisibility;
import com.tencent.supersonic.chat.domain.utils.DefaultSemanticInternalUtils;
import com.tencent.supersonic.common.exception.CommonException;
import com.tencent.supersonic.common.result.ResultData;
import com.tencent.supersonic.common.result.ReturnCode;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class RemoteSemanticLayerImpl implements SemanticLayer {

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
        DefaultSemanticInternalUtils defaultSemanticInternalUtils = ContextUtils.getBean(
                DefaultSemanticInternalUtils.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        defaultSemanticInternalUtils.fillToken(headers);
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
        DefaultSemanticInternalUtils defaultSemanticInternalUtils = ContextUtils.getBean(
                DefaultSemanticInternalUtils.class);
        HttpHeaders headers = new HttpHeaders();
        headers.set(UserConstants.INTERNAL, TRUE_LOWER);
        headers.setContentType(MediaType.APPLICATION_JSON);
        defaultSemanticInternalUtils.fillToken(headers);
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
    public List<DomainSchemaResp> fetchDomainSchema(List<Long> ids) {
        return domainSchemaCache.get(String.valueOf(ids), () -> {
            List<DomainSchemaResp> data = fetchDomainSchemaAll(ids);
            fillEntityNameAndFilterBlackElement(data);
            return data;
        });
    }

    @Override
    public DomainSchemaResp getDomainSchemaInfo(Long domain) {
        List<Long> ids = new ArrayList<>();
        ids.add(domain);
        List<DomainSchemaResp> domainSchemaResps = fetchDomainSchema(ids);
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
        return fetchDomainSchema(ids);
    }

    public DomainSchemaResp fillEntityNameAndFilterBlackElement(DomainSchemaResp domainSchemaResp) {
        if (Objects.isNull(domainSchemaResp) || Objects.isNull(domainSchemaResp.getId())) {
            return domainSchemaResp;
        }
        ChatConfigInfo chaConfigInfo = getConfigBaseInfo(domainSchemaResp.getId());

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

    private void filterBlackMetric(DomainSchemaResp domainSchemaResp, ChatConfigInfo chaConfigInfo) {
        ItemVisibility visibility = chaConfigInfo.getVisibility();
        if (Objects.nonNull(chaConfigInfo) && Objects.nonNull(visibility)
                && !CollectionUtils.isEmpty(visibility.getBlackMetricIdList())
                && !CollectionUtils.isEmpty(domainSchemaResp.getMetrics())) {
            List<MetricSchemaResp> metric4Chat = domainSchemaResp.getMetrics().stream()
                    .filter(metric -> !visibility.getBlackMetricIdList().contains(metric.getId()))
                    .collect(Collectors.toList());
            domainSchemaResp.setMetrics(metric4Chat);
        }
    }

    private void filterBlackDim(DomainSchemaResp domainSchemaResp, ChatConfigInfo chatConfigInfo) {
        ItemVisibility visibility = chatConfigInfo.getVisibility();
        if (Objects.nonNull(chatConfigInfo) && Objects.nonNull(visibility)
                && !CollectionUtils.isEmpty(visibility.getBlackDimIdList())
                && !CollectionUtils.isEmpty(domainSchemaResp.getDimensions())) {
            List<DimSchemaResp> dim4Chat = domainSchemaResp.getDimensions().stream()
                    .filter(dim -> !visibility.getBlackDimIdList().contains(dim.getId()))
                    .collect(Collectors.toList());
            domainSchemaResp.setDimensions(dim4Chat);
        }
    }

    private void fillEntityNamesInfo(DomainSchemaResp domainSchemaResp, ChatConfigInfo chatConfigInfo) {
        if (Objects.nonNull(chatConfigInfo) && Objects.nonNull(chatConfigInfo.getEntity())
                && !CollectionUtils.isEmpty(chatConfigInfo.getEntity().getNames())) {
            domainSchemaResp.setEntityNames(chatConfigInfo.getEntity().getNames());
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

    public ChatConfigInfo getConfigBaseInfo(Long domain) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return defaultSemanticConfig.getChaConfigService().fetchConfigByDomainId(domain);
    }

}
