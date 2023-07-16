package com.tencent.supersonic.chat.infrastructure.semantic;

import com.github.pagehelper.PageInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.domain.pojo.config.ChatAggConfig;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatDetailConfig;
import com.tencent.supersonic.chat.domain.pojo.config.ItemVisibility;
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
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.core.domain.MetricService;
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

    private S2ThreadContext s2ThreadContext;

    private DomainService domainService;

    private DimensionService dimensionService;

    private MetricService metricService;

//    public LocalSemanticLayerImpl(DomainService domainService){
//        this.domainService=domainService;
//    }

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
        ChatConfigResp chatConfigResp = getConfigBaseInfo(domainSchemaResp.getId());

        // fill entity names
        fillEntityNamesInfo(domainSchemaResp, chatConfigResp);

        // filter black element
        filterBlackDim(domainSchemaResp, chatConfigResp);
        filterBlackMetric(domainSchemaResp, chatConfigResp);
        return domainSchemaResp;
    }

    public void fillEntityNameAndFilterBlackElement(List<DomainSchemaResp> domainSchemaRespList) {
        if (!CollectionUtils.isEmpty(domainSchemaRespList)) {
            domainSchemaRespList.stream()
                    .forEach(domainSchemaResp -> fillEntityNameAndFilterBlackElement(domainSchemaResp));
        }
    }

    private void filterBlackMetric(DomainSchemaResp domainSchemaResp, ChatConfigResp chatConfigResp) {
        ItemVisibility visibility = generateFinalVisibility(chatConfigResp);
        if (Objects.nonNull(chatConfigResp) && Objects.nonNull(visibility)
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

    private void filterBlackDim(DomainSchemaResp domainSchemaResp, ChatConfigResp chatConfigResp) {
        ItemVisibility visibility = generateFinalVisibility(chatConfigResp);
        if (Objects.nonNull(chatConfigResp) && Objects.nonNull(visibility)
                && !CollectionUtils.isEmpty(visibility.getBlackDimIdList())
                && !CollectionUtils.isEmpty(domainSchemaResp.getDimensions())) {
            List<DimSchemaResp> dim4Chat = domainSchemaResp.getDimensions().stream()
                    .filter(dim -> !visibility.getBlackDimIdList().contains(dim.getId()))
                    .collect(Collectors.toList());
            domainSchemaResp.setDimensions(dim4Chat);
        }
    }

    private void fillEntityNamesInfo(DomainSchemaResp domainSchemaResp, ChatConfigResp chatConfigResp) {
        if (Objects.nonNull(chatConfigResp) && Objects.nonNull(chatConfigResp.getChatDetailConfig())
                && Objects.nonNull(chatConfigResp.getChatDetailConfig().getEntity())
                && !CollectionUtils.isEmpty(chatConfigResp.getChatDetailConfig().getEntity().getNames())) {
            domainSchemaResp.setEntityNames(chatConfigResp.getChatDetailConfig().getEntity().getNames());
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
        s2ThreadContext = ContextUtils.getBean(S2ThreadContext.class);
        ThreadContext threadContext = s2ThreadContext.get();
        domainService = ContextUtils.getBean(DomainService.class);
        return domainService.getDomainListForViewer(threadContext.getUserName());
    }

    @Override
    public List<DomainResp> getDomainListForAdmin() {
        domainService = ContextUtils.getBean(DomainService.class);
        s2ThreadContext = ContextUtils.getBean(S2ThreadContext.class);
        ThreadContext threadContext = s2ThreadContext.get();
        return domainService.getDomainListForAdmin(threadContext.getUserName());
    }

    @Override
    public PageInfo<DimensionResp> queryDimensionPage(PageDimensionReq pageDimensionCmd) {
        dimensionService = ContextUtils.getBean(DimensionService.class);
        return dimensionService.queryDimension(pageDimensionCmd);
    }

    @Override
    public PageInfo<MetricResp> queryMetricPage(PageMetricReq pageMetricCmd) {
        metricService = ContextUtils.getBean(MetricService.class);
        return metricService.queryMetric(pageMetricCmd);
    }

}
