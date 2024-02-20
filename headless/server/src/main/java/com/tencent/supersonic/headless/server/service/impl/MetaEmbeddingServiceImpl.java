package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ComponentFactory;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQuery;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.common.util.embedding.S2EmbeddingStore;
import com.tencent.supersonic.headless.server.service.MetaEmbeddingService;
import com.tencent.supersonic.headless.server.service.ViewService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MetaEmbeddingServiceImpl implements MetaEmbeddingService {

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();
    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Autowired
    private ViewService viewService;

    @Override
    public List<RetrieveQueryResult> retrieveQuery(List<Long> viewIds, RetrieveQuery retrieveQuery, int num) {
        // viewIds->modelIds
        Map<Long, List<Long>> modelIdToViewIds = viewService.getModelIdToViewIds(viewIds);
        Set<Long> allModels = modelIdToViewIds.keySet();

        if (CollectionUtils.isNotEmpty(allModels) && allModels.size() == 1) {
            Map<String, String> filterCondition = new HashMap<>();
            filterCondition.put("modelId", allModels.stream().findFirst().get().toString());
            retrieveQuery.setFilterCondition(filterCondition);
        }

        String collectionName = embeddingConfig.getMetaCollectionName();
        List<RetrieveQueryResult> resultList = s2EmbeddingStore.retrieveQuery(collectionName, retrieveQuery, num);
        if (CollectionUtils.isEmpty(resultList)) {
            return new ArrayList<>();
        }
        //filter by modelId
        if (CollectionUtils.isEmpty(allModels)) {
            return resultList;
        }
        return resultList.stream()
                .map(retrieveQueryResult -> {
                    List<Retrieval> retrievals = retrieveQueryResult.getRetrieval();
                    if (CollectionUtils.isEmpty(retrievals)) {
                        return retrieveQueryResult;
                    }
                    //filter by modelId
                    retrievals.removeIf(retrieval -> {
                        Long modelId = Retrieval.getLongId(retrieval.getMetadata().get("modelId"));
                        if (Objects.isNull(modelId)) {
                            return CollectionUtils.isEmpty(allModels);
                        }
                        return !allModels.contains(modelId);
                    });
                    //add viewId
                    retrievals = retrievals.stream().flatMap(retrieval -> {
                        Long modelId = Retrieval.getLongId(retrieval.getMetadata().get("modelId"));
                        List<Long> viewIdsByModelId = modelIdToViewIds.get(modelId);
                        if (!CollectionUtils.isEmpty(viewIdsByModelId)) {
                            Set<Retrieval> result = new HashSet<>();
                            for (Long viewId : viewIdsByModelId) {
                                Retrieval retrievalNew = new Retrieval();
                                BeanUtils.copyProperties(retrieval, retrievalNew);
                                retrievalNew.getMetadata().putIfAbsent("viewId", viewId + Constants.UNDERLINE);
                                result.add(retrievalNew);
                            }
                            return result.stream();
                        }
                        Set<Retrieval> result = new HashSet<>();
                        result.add(retrieval);
                        return result.stream();
                    }).collect(Collectors.toList());
                    retrieveQueryResult.setRetrieval(retrievals);
                    return retrieveQueryResult;
                })
                .filter(retrieveQueryResult -> CollectionUtils.isNotEmpty(retrieveQueryResult.getRetrieval()))
                .collect(Collectors.toList());
    }
}