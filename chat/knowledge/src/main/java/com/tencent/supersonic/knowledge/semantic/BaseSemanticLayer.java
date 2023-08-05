package com.tencent.supersonic.knowledge.semantic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.DomainSchema;
import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.semantic.api.model.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

    protected abstract List<DomainSchemaResp> doFetchDomainSchema(List<Long> ids);
}
