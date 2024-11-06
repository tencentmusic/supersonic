package com.tencent.supersonic.headless.api.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class SchemaMapInfo implements Serializable {

    private final Map<Long, List<SchemaElementMatch>> dataSetElementMatches = new HashMap<>();

    public boolean isEmpty() {
        return dataSetElementMatches.keySet().isEmpty();
    }

    public Set<Long> getMatchedDataSetInfos() {
        return dataSetElementMatches.keySet();
    }

    public List<SchemaElementMatch> getMatchedElements(Long dataSet) {
        return dataSetElementMatches.getOrDefault(dataSet, Lists.newArrayList());
    }

    public void setMatchedElements(Long dataSet, List<SchemaElementMatch> elementMatches) {
        dataSetElementMatches.put(dataSet, elementMatches);
    }

    public void addMatchedElements(SchemaMapInfo schemaMapInfo) {
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMapInfo.dataSetElementMatches
                .entrySet()) {
            Long dataSet = entry.getKey();
            List<SchemaElementMatch> newMatches = entry.getValue();

            if (dataSetElementMatches.containsKey(dataSet)) {
                List<SchemaElementMatch> existingMatches = dataSetElementMatches.get(dataSet);
                Set<SchemaElementMatch> mergedMatches = new HashSet<>(existingMatches);
                mergedMatches.addAll(newMatches);
                dataSetElementMatches.put(dataSet, new ArrayList<>(mergedMatches));
            } else {
                dataSetElementMatches.put(dataSet, new ArrayList<>(new HashSet<>(newMatches)));
            }
        }
    }

    @JsonIgnore
    public List<SchemaElement> getTermDescriptionToMap() {
        List<SchemaElement> termElements = Lists.newArrayList();
        for (Long dataSetId : getDataSetElementMatches().keySet()) {
            List<SchemaElementMatch> matchedElements = getMatchedElements(dataSetId);
            for (SchemaElementMatch schemaElementMatch : matchedElements) {
                if (SchemaElementType.TERM.equals(schemaElementMatch.getElement().getType())
                        && schemaElementMatch.isFullMatched()) {
                    termElements.add(schemaElementMatch.getElement());
                }
            }
        }
        return termElements;
    }
}
