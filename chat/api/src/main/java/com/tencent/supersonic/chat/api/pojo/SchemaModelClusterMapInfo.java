package com.tencent.supersonic.chat.api.pojo;

import com.clickhouse.client.internal.apache.commons.compress.utils.Lists;
import com.tencent.supersonic.common.pojo.ModelCluster;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class SchemaModelClusterMapInfo {

    private Map<String, List<SchemaElementMatch>> modelElementMatches = new HashMap<>();

    public Set<String> getMatchedModelClusters() {
        return modelElementMatches.keySet();
    }

    public List<SchemaElementMatch> getMatchedElements(Long modelId) {
        for (String key : modelElementMatches.keySet()) {
            if (ModelCluster.getModelIdFromKey(key).contains(modelId)) {
                return modelElementMatches.get(key);
            }
        }
        return Lists.newArrayList();
    }

    public List<SchemaElementMatch> getMatchedElements(String modelCluster) {
        return modelElementMatches.get(modelCluster);
    }

    public Map<String, List<SchemaElementMatch>> getModelElementMatches() {
        return modelElementMatches;
    }

    public Map<String, List<SchemaElementMatch>> getElementMatchesByModelIds(Set<Long> modelIds) {
        if (CollectionUtils.isEmpty(modelIds)) {
            return modelElementMatches;
        }
        Map<String, List<SchemaElementMatch>> modelElementMatchesFiltered = new HashMap<>();
        for (String key : modelElementMatches.keySet()) {
            for (Long modelId : modelIds) {
                if (ModelCluster.getModelIdFromKey(key).contains(modelId)) {
                    modelElementMatchesFiltered.put(key, modelElementMatches.get(key));
                }
            }
        }
        return modelElementMatchesFiltered;
    }

    public void setModelElementMatches(Map<String, List<SchemaElementMatch>> modelElementMatches) {
        this.modelElementMatches = modelElementMatches;
    }

    public void setMatchedElements(String modelCluster, List<SchemaElementMatch> elementMatches) {
        modelElementMatches.put(modelCluster, elementMatches);
    }
}
