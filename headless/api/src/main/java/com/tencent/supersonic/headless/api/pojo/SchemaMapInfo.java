package com.tencent.supersonic.headless.api.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchemaMapInfo {

    private Map<Long, List<SchemaElementMatch>> dataSetElementMatches = new HashMap<>();

    public Set<Long> getMatchedDataSetInfos() {
        return dataSetElementMatches.keySet();
    }

    public List<SchemaElementMatch> getMatchedElements(Long dataSet) {
        return dataSetElementMatches.getOrDefault(dataSet, Lists.newArrayList());
    }

    public Map<Long, List<SchemaElementMatch>> getDataSetElementMatches() {
        return dataSetElementMatches;
    }

    public void setMatchedElements(Long dataSet, List<SchemaElementMatch> elementMatches) {
        dataSetElementMatches.put(dataSet, elementMatches);
    }

    @JsonIgnore
    public List<SchemaElement> getTermDescriptionToMap() {
        List<SchemaElement> termElements = Lists.newArrayList();
        for (Long dataSetId : getDataSetElementMatches().keySet()) {
            List<SchemaElementMatch> matchedElements = getMatchedElements(dataSetId);
            for (SchemaElementMatch schemaElementMatch : matchedElements) {
                if (SchemaElementType.TERM.equals(schemaElementMatch.getElement().getType())
                        && schemaElementMatch.isFullMatched()
                        && !schemaElementMatch.getElement().isDescriptionMapped()) {
                    termElements.add(schemaElementMatch.getElement());
                }
            }
        }
        return termElements;
    }

    public boolean needContinueMap() {
        return CollectionUtils.isNotEmpty(getTermDescriptionToMap());
    }
}
