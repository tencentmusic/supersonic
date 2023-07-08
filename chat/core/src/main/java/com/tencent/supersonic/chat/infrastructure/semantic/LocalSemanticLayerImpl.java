package com.tencent.supersonic.chat.infrastructure.semantic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ItemVisibility;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.common.util.json.JsonUtil;
import com.tencent.supersonic.semantic.api.core.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QuerySqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.query.domain.SchemaService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import com.tencent.supersonic.semantic.query.domain.QueryService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class LocalSemanticLayerImpl implements SemanticLayer {

    private static final Cache<String, List<DomainSchemaResp>> domainSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();
    private SchemaService schemaService;

    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructReq, User user) {
        deletionDuplicated(queryStructReq);
        onlyQueryFirstMetric(queryStructReq);
        try {
            QueryService queryService = ContextUtils.getBean(QueryService.class);
            QueryResultWithSchemaResp queryResultWithSchemaResp = queryService.queryByStruct(queryStructReq, user);
            return queryResultWithSchemaResp;
        } catch (Exception e) {
            log.info("queryByStruct has an exception:{}", e.toString());
        }
        return null;
    }

    @Override
    public QueryResultWithSchemaResp queryBySql(QuerySqlReq querySqlReq, User user) {
        try {
            QueryService queryService = ContextUtils.getBean(QueryService.class);
            Object object = queryService.queryBySql(querySqlReq, user);
            QueryResultWithSchemaResp queryResultWithSchemaResp = JsonUtil.toObject(JsonUtil.toString(object),
                    QueryResultWithSchemaResp.class);
            return queryResultWithSchemaResp;
        } catch (Exception e) {
            log.info("queryByStruct has an exception:{}", e.toString());
        }
        return null;
    }

    public List<DomainSchemaResp> fetchDomainSchemaAll(List<Long> ids) {

        DomainSchemaFilterReq filter = new DomainSchemaFilterReq();
        filter.setDomainIds(ids);
        User user = new User(1L, "admin", "admin", "admin@email");
        schemaService = ContextUtils.getBean(SchemaService.class);
        return schemaService.fetchDomainSchema(filter, user);
    }


    @SneakyThrows
    public List<DomainSchemaResp> fetchDomainSchema(List<Long> ids) {
//        return domainSchemaCache.get(String.valueOf(ids), () -> {
//            List<DomainSchemaResp> data = fetchDomainSchemaAll(ids);
//            fillEntityNameAndFilterBlackElement(data);
//            return data;
//        });

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
            log.info("multi metric in aggregators:{} , only query first one", queryStructReq.getAggregators());
            queryStructReq.setAggregators(queryStructReq.getAggregators().subList(0, 1));
        }
    }

    public ChatConfigInfo getConfigBaseInfo(Long domain) {
        DefaultSemanticConfig defaultSemanticConfig = ContextUtils.getBean(DefaultSemanticConfig.class);
        return defaultSemanticConfig.getChaConfigService().fetchConfigByDomainId(domain);
    }

}
