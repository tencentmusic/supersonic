package com.tencent.supersonic.chat.core.query.semantic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.chat.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.response.DataSetSchemaResp;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseSemanticInterpreter implements SemanticInterpreter {

    protected final Cache<String, List<DataSetSchemaResp>> dataSetSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    @SneakyThrows
    public List<DataSetSchemaResp> fetchDataSetSchema(List<Long> ids, Boolean cacheEnable) {
        if (cacheEnable) {
            return dataSetSchemaCache.get(String.valueOf(ids), () -> {
                List<DataSetSchemaResp> data = doFetchDataSetSchema(ids);
                dataSetSchemaCache.put(String.valueOf(ids), data);
                return data;
            });
        }
        return doFetchDataSetSchema(ids);
    }

    @Override
    public DataSetSchema getDataSetSchema(Long dataSetId, Boolean cacheEnable) {
        List<Long> ids = new ArrayList<>();
        ids.add(dataSetId);
        List<DataSetSchemaResp> dataSetSchemaResps = fetchDataSetSchema(ids, cacheEnable);
        if (!CollectionUtils.isEmpty(dataSetSchemaResps)) {
            Optional<DataSetSchemaResp> dataSetSchemaResp = dataSetSchemaResps.stream()
                    .filter(d -> d.getId().equals(dataSetId)).findFirst();
            if (dataSetSchemaResp.isPresent()) {
                DataSetSchemaResp dataSetSchema = dataSetSchemaResp.get();
                return DataSetSchemaBuilder.build(dataSetSchema);
            }
        }
        return null;
    }

    @Override
    public List<DataSetSchema> getDataSetSchema() {
        return getDataSetSchema(new ArrayList<>());
    }

    @Override
    public List<DataSetSchema> getDataSetSchema(List<Long> ids) {
        List<DataSetSchema> domainSchemaList = new ArrayList<>();

        for (DataSetSchemaResp resp : fetchDataSetSchema(ids, true)) {
            domainSchemaList.add(DataSetSchemaBuilder.build(resp));
        }

        return domainSchemaList;
    }

    protected abstract List<DataSetSchemaResp> doFetchDataSetSchema(List<Long> ids);

}
