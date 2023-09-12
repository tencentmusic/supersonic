package com.tencent.supersonic.knowledge.semantic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
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

    protected final Cache<String, List<ModelSchemaResp>> modelSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    protected ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>> structTypeRef =
            new ParameterizedTypeReference<ResultData<QueryResultWithSchemaResp>>() {
            };

    @SneakyThrows
    public List<ModelSchemaResp> fetchModelSchema(List<Long> ids, Boolean cacheEnable) {
        if (cacheEnable) {
            return modelSchemaCache.get(String.valueOf(ids), () -> {
                List<ModelSchemaResp> data = doFetchModelSchema(ids);
                return data;
            });
        }
        List<ModelSchemaResp> data = doFetchModelSchema(ids);
        return data;
    }

    @Override
    public ModelSchema getModelSchema(Long domain, Boolean cacheEnable) {
        List<Long> ids = new ArrayList<>();
        ids.add(domain);
        List<ModelSchemaResp> modelSchemaResps = fetchModelSchema(ids, cacheEnable);
        if (!CollectionUtils.isEmpty(modelSchemaResps)) {
            Optional<ModelSchemaResp> modelSchemaResp = modelSchemaResps.stream()
                    .filter(d -> d.getId().equals(domain)).findFirst();
            if (modelSchemaResp.isPresent()) {
                ModelSchemaResp modelSchema = modelSchemaResp.get();
                return ModelSchemaBuilder.build(modelSchema);
            }
        }
        return null;
    }

    @Override
    public List<ModelSchema> getModelSchema() {
        return getModelSchema(new ArrayList<>());
    }

    @Override
    public List<ModelSchema> getModelSchema(List<Long> ids) {
        List<ModelSchema> domainSchemaList = new ArrayList<>();

        for (ModelSchemaResp resp : fetchModelSchema(ids, true)) {
            domainSchemaList.add(ModelSchemaBuilder.build(resp));
        }

        return domainSchemaList;
    }

    protected abstract List<ModelSchemaResp> doFetchModelSchema(List<Long> ids);
}
