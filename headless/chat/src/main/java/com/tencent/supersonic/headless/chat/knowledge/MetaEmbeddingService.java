package com.tencent.supersonic.headless.chat.knowledge;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.Constants;
import dev.langchain4j.store.embedding.ComponentFactory;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.S2EmbeddingStore;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
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
public class MetaEmbeddingService {

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();
    @Autowired
    private EmbeddingConfig embeddingConfig;

    public List<RetrieveQueryResult> retrieveQuery(RetrieveQuery retrieveQuery, int num,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        // dataSetIds->modelIds
        Set<Long> allModels = NatureHelper.getModelIds(modelIdToDataSetIds, detectDataSetIds);

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
                    //add dataSetId
                    retrievals = retrievals.stream().flatMap(retrieval -> {
                        Long modelId = Retrieval.getLongId(retrieval.getMetadata().get("modelId"));
                        List<Long> dataSetIdsByModelId = modelIdToDataSetIds.get(modelId);
                        if (!CollectionUtils.isEmpty(dataSetIdsByModelId)) {
                            Set<Retrieval> result = new HashSet<>();
                            for (Long dataSetId : dataSetIdsByModelId) {
                                Retrieval retrievalNew = new Retrieval();
                                BeanUtils.copyProperties(retrieval, retrievalNew);
                                retrievalNew.getMetadata().putIfAbsent("dataSetId", dataSetId + Constants.UNDERLINE);
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