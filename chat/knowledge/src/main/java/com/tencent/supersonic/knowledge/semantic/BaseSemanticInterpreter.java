package com.tencent.supersonic.knowledge.semantic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public abstract class BaseSemanticInterpreter implements SemanticInterpreter {

    protected final Cache<String, List<ModelSchemaResp>> modelSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    @SneakyThrows
    public List<ModelSchemaResp> fetchModelSchema(List<Long> ids, Boolean cacheEnable) {
        if (cacheEnable) {
            return modelSchemaCache.get(String.valueOf(ids), () -> {
                List<ModelSchemaResp> data = doFetchModelSchema(ids);
                modelSchemaCache.put(String.valueOf(ids), data);
                return data;
            });
        }
        List<ModelSchemaResp> data = doFetchModelSchema(ids);
        return data;
    }

    @Override
    public ModelSchema getModelSchema(Long model, Boolean cacheEnable) {
        List<Long> ids = new ArrayList<>();
        ids.add(model);
        List<ModelSchemaResp> modelSchemaResps = fetchModelSchema(ids, cacheEnable);
        if (!CollectionUtils.isEmpty(modelSchemaResps)) {
            Optional<ModelSchemaResp> modelSchemaResp = modelSchemaResps.stream()
                    .filter(d -> d.getId().equals(model)).findFirst();
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
