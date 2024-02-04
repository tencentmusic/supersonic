package com.tencent.supersonic.chat.api.pojo;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchemaMapInfo {

    private Map<Long, List<SchemaElementMatch>> viewElementMatches = new HashMap<>();

    public Set<Long> getMatchedViewInfos() {
        return viewElementMatches.keySet();
    }

    public List<SchemaElementMatch> getMatchedElements(Long view) {
        return viewElementMatches.getOrDefault(view, Lists.newArrayList());
    }

    public Map<Long, List<SchemaElementMatch>> getViewElementMatches() {
        return viewElementMatches;
    }

    public void setViewElementMatches(Map<Long, List<SchemaElementMatch>> viewElementMatches) {
        this.viewElementMatches = viewElementMatches;
    }

    public void setMatchedElements(Long view, List<SchemaElementMatch> elementMatches) {
        viewElementMatches.put(view, elementMatches);
    }
}
