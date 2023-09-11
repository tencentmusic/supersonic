package com.tencent.supersonic.chat.api.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchemaMapInfo {

    private Map<Long, List<SchemaElementMatch>> modelElementMatches = new HashMap<>();

    public Set<Long> getMatchedModels() {
        return modelElementMatches.keySet();
    }

    public List<SchemaElementMatch> getMatchedElements(Long model) {
        return modelElementMatches.get(model);
    }

    public Map<Long, List<SchemaElementMatch>> getModelElementMatches() {
        return modelElementMatches;
    }

    public void setModelElementMatches(Map<Long, List<SchemaElementMatch>> modelElementMatches) {
        this.modelElementMatches = modelElementMatches;
    }

    public void setMatchedElements(Long model, List<SchemaElementMatch> elementMatches) {
        modelElementMatches.put(model, elementMatches);
    }
}
