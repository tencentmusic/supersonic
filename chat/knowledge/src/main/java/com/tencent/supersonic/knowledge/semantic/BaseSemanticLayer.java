package com.tencent.supersonic.knowledge.semantic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.DomainSchema;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.semantic.api.model.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;

@Slf4j
public abstract class BaseSemanticLayer implements SemanticLayer {

    protected final Cache<String, List<DomainSchemaResp>> domainSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    protected ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>> structTypeRef =
            new ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>>() {
            };

    @SneakyThrows
    public List<DomainSchemaResp> fetchDomainSchema(List<Long> ids, Boolean cacheEnable) {
        if (cacheEnable) {
            return domainSchemaCache.get(String.valueOf(ids), () -> {
                List<DomainSchemaResp> data = doFetchDomainSchema(ids);
                return data;
            });
        }
        List<DomainSchemaResp> data = doFetchDomainSchema(ids);
        return data;
    }

    @Override
    public DomainSchema getDomainSchema(Long domain, Boolean cacheEnable) {
        List<Long> ids = new ArrayList<>();
        ids.add(domain);
        List<DomainSchemaResp> domainSchemaResps = fetchDomainSchema(ids, cacheEnable);
        if (!CollectionUtils.isEmpty(domainSchemaResps)) {
            Optional<DomainSchemaResp> domainSchemaResp = domainSchemaResps.stream()
                    .filter(d -> d.getId().equals(domain)).findFirst();
            if (domainSchemaResp.isPresent()) {
                DomainSchemaResp domainSchema = domainSchemaResp.get();
                return DomainSchemaBuilder.build(domainSchema);
            }
        }
        return null;
    }

    @Override
    public List<DomainSchema> getDomainSchema() {
        return getDomainSchema(new ArrayList<>());
    }

    @Override
    public List<DomainSchema> getDomainSchema(List<Long> ids) {
        List<DomainSchema> domainSchemaList = new ArrayList<>();

        for(DomainSchemaResp resp : fetchDomainSchema(ids, true)) {
            domainSchemaList.add(DomainSchemaBuilder.build(resp));
        }

        return domainSchemaList;
    }

    protected void deletionDuplicated(QueryStructReq queryStructReq) {
        if (!CollectionUtils.isEmpty(queryStructReq.getGroups()) && queryStructReq.getGroups().size() > 1) {
            Set<String> groups = new HashSet<>();
            groups.addAll(queryStructReq.getGroups());
            queryStructReq.getGroups().clear();
            queryStructReq.getGroups().addAll(groups);
        }
    }

    protected void onlyQueryFirstMetric(QueryStructReq queryStructReq) {
        if (!CollectionUtils.isEmpty(queryStructReq.getAggregators()) && queryStructReq.getAggregators().size() > 1) {
            log.info("multi metric in aggregators:{} , only query first one", queryStructReq.getAggregators());
            List<Aggregator> aggregators = queryStructReq.getAggregators().subList(0, 1);
            List<String> excludeAggregators = queryStructReq.getAggregators().stream().map(a -> a.getColumn())
                    .filter(a -> !a.equals(aggregators.get(0).getColumn())).collect(
                            Collectors.toList());
            queryStructReq.setAggregators(aggregators);
            List<Order> orders = queryStructReq.getOrders().stream()
                    .filter(o -> !excludeAggregators.contains(o.getColumn())).collect(
                            Collectors.toList());
            log.info("multi metric in orders:{} ", queryStructReq.getOrders());
            queryStructReq.setOrders(orders);

        }
    }

    protected abstract List<DomainSchemaResp> doFetchDomainSchema(List<Long> ids);
}
