package com.tencent.supersonic.chat.core.knowledge.semantic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.headless.api.pojo.response.ViewSchemaResp;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseSemanticInterpreter implements SemanticInterpreter {

    protected final Cache<String, List<ViewSchemaResp>> viewSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    @SneakyThrows
    public List<ViewSchemaResp> fetchViewSchema(List<Long> ids, Boolean cacheEnable) {
        if (cacheEnable) {
            return viewSchemaCache.get(String.valueOf(ids), () -> {
                List<ViewSchemaResp> data = doFetchViewSchema(ids);
                viewSchemaCache.put(String.valueOf(ids), data);
                return data;
            });
        }
        return doFetchViewSchema(ids);
    }

    @Override
    public ViewSchema getViewSchema(Long viewId, Boolean cacheEnable) {
        List<Long> ids = new ArrayList<>();
        ids.add(viewId);
        List<ViewSchemaResp> viewSchemaResps = fetchViewSchema(ids, cacheEnable);
        if (!CollectionUtils.isEmpty(viewSchemaResps)) {
            Optional<ViewSchemaResp> viewSchemaResp = viewSchemaResps.stream()
                    .filter(d -> d.getId().equals(viewId)).findFirst();
            if (viewSchemaResp.isPresent()) {
                ViewSchemaResp viewSchema = viewSchemaResp.get();
                return ViewSchemaBuilder.build(viewSchema);
            }
        }
        return null;
    }

    @Override
    public List<ViewSchema> getViewSchema() {
        return getViewSchema(new ArrayList<>());
    }

    @Override
    public List<ViewSchema> getViewSchema(List<Long> ids) {
        List<ViewSchema> domainSchemaList = new ArrayList<>();

        for (ViewSchemaResp resp : fetchViewSchema(ids, true)) {
            domainSchemaList.add(ViewSchemaBuilder.build(resp));
        }

        return domainSchemaList;
    }

    protected abstract List<ViewSchemaResp> doFetchViewSchema(List<Long> ids);

}
