package com.tencent.supersonic.headless.chat.knowledge;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.service.EmbeddingService;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class MetaEmbeddingService {

    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private EmbeddingConfig embeddingConfig;

    public List<RetrieveQueryResult> retrieveQuery(RetrieveQuery retrieveQuery, int num,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        // dataSetIds->modelIds
        Set<Long> allModels = NatureHelper.getModelIds(modelIdToDataSetIds, detectDataSetIds);

        if (CollectionUtils.isNotEmpty(allModels)) {
            Map<String, Object> filterCondition = new HashMap<>();
            filterCondition.put("modelId",
                    allModels.stream().map(modelId -> modelId + DictWordType.NATURE_SPILT)
                            .collect(Collectors.toList()));
            retrieveQuery.setFilterCondition(filterCondition);
        }

        String collectionName = embeddingConfig.getMetaCollectionName();
        List<RetrieveQueryResult> resultList =
                embeddingService.retrieveQuery(collectionName, retrieveQuery, num);
        if (CollectionUtils.isEmpty(resultList)) {
            return new ArrayList<>();
        }
        // Filter and process query results.
        return resultList.stream()
                .map(result -> getRetrieveQueryResult(modelIdToDataSetIds, result))
                .filter(result -> CollectionUtils.isNotEmpty(result.getRetrieval()))
                .collect(Collectors.toList());
    }

    private static RetrieveQueryResult getRetrieveQueryResult(
            Map<Long, List<Long>> modelIdToDataSetIds, RetrieveQueryResult result) {
        List<Retrieval> retrievals = result.getRetrieval();
        if (CollectionUtils.isEmpty(retrievals)) {
            return result;
        }
        // Process each Retrieval object.
        List<Retrieval> updatedRetrievals = retrievals.stream().flatMap(retrieval -> {
            Long modelId = Retrieval.getLongId(retrieval.getMetadata().get("modelId"));
            List<Long> dataSetIds = modelIdToDataSetIds.get(modelId);

            if (CollectionUtils.isEmpty(dataSetIds)) {
                return Stream.of(retrieval);
            }

            return dataSetIds.stream().map(dataSetId -> {
                Retrieval newRetrieval = new Retrieval();
                BeanUtils.copyProperties(retrieval, newRetrieval);
                newRetrieval.getMetadata().putIfAbsent("dataSetId",
                        dataSetId + Constants.UNDERLINE);
                return newRetrieval;
            });
        }).collect(Collectors.toList());
        result.setRetrieval(updatedRetrievals);
        return result;
    }
}
