package com.tencent.supersonic.chat.infrastructure.semantic;

import static com.tencent.supersonic.common.constant.Constants.TRUE_LOWER;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authentication.constant.UserConstants;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.semantic.api.core.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.chat.application.ConfigServiceImpl;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DefaultSemanticLayerImpl implements SemanticLayer {

    private final Logger logger = LoggerFactory.getLogger(DefaultSemanticLayerImpl.class);

    @Value("${semantic.url.prefix:http://localhost:8081}")
    private String semanticUrl;

    @Value("${searchByStruct.path:/api/semantic/query/struct}")
    private String searchByStructPath;

    @Value("${fetchDomainSchemaPath.path:/api/semantic/schema}")
    private String fetchDomainSchemaPath;

    @Autowired
    private DefaultSemanticInternalUtils defaultSemanticInternalUtils;

    private ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>> structTypeRef =
            new ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>>() {
            };

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfigServiceImpl chaConfigService;

    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructReq, User user) {
        deletionDuplicated(queryStructReq);
        onlyQueryFirstMetric(queryStructReq);
        return searchByStruct(semanticUrl + searchByStructPath, queryStructReq);
    }

    public QueryResultWithSchemaResp searchByStruct(String url, QueryStructReq queryStructReq) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        defaultSemanticInternalUtils.fillToken(headers);
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(url).build().encode().toUri();
        Gson gson = new Gson();
        HttpEntity<String> entity = new HttpEntity<>(gson.toJson(queryStructReq), headers);
        logger.info("searchByStruct {}", entity.getBody());
        ResultData<QueryResultWithSchemaResp> responseBody;
        try {
            ResponseEntity<ResultData<QueryResultWithSchemaResp>> responseEntity = restTemplate.exchange(requestUrl,
                    HttpMethod.POST, entity, structTypeRef);
            responseBody = responseEntity.getBody();
            logger.debug("ApiResponse<QueryResultWithColumns> responseBody:{}", responseBody);
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
            throw new RuntimeException("search semantic struct interface error", e);
        }
        throw new CommonException(responseBody.getCode(), responseBody.getMsg());
    }

    public List<DomainSchemaResp> fetchDomainSchemaAll(List<Long> ids) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(UserConstants.INTERNAL, TRUE_LOWER);
        headers.setContentType(MediaType.APPLICATION_JSON);
        defaultSemanticInternalUtils.fillToken(headers);
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(semanticUrl + fetchDomainSchemaPath).build().encode().toUri();
        DomainSchemaFilterReq filter = new DomainSchemaFilterReq();
        filter.setDomainIds(ids);
        ParameterizedTypeReference<ResultData<List<DomainSchemaResp>>> responseTypeRef =
                new ParameterizedTypeReference<ResultData<List<DomainSchemaResp>>>() {
                };

        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(filter), headers);
        try {
            ResponseEntity<ResultData<List<DomainSchemaResp>>> responseEntity = restTemplate.exchange(requestUrl,
                    HttpMethod.POST, entity, responseTypeRef);
            ResultData<List<DomainSchemaResp>> responseBody = responseEntity.getBody();
            logger.debug("ApiResponse<fetchDomainSchema> responseBody:{}", responseBody);
            if (ReturnCode.SUCCESS.getCode() == responseBody.getCode()) {
                List<DomainSchemaResp> data = responseBody.getData();
                return data;
            }
        } catch (Exception e) {
            throw new RuntimeException("fetchDomainSchema interface error", e);
        }
        throw new RuntimeException("fetchDomainSchema interface error");
    }


    public List<DomainSchemaResp> fetchDomainSchema(List<Long> ids) {
        List<DomainSchemaResp> data = fetchDomainSchemaAll(ids);
        fillEntityNameAndFilterBlackElement(data);
        return data;
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
            logger.info("multi metric in aggregators:{} , only query first one", queryStructReq.getAggregators());
            queryStructReq.setAggregators(queryStructReq.getAggregators().subList(0, 1));
        }
    }

    public ChatConfigInfo getConfigBaseInfo(Long domain) {
        return chaConfigService.fetchConfigByDomainId(domain);
    }

}
