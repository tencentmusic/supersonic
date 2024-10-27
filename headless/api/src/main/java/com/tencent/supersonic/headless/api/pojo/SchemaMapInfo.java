package com.tencent.supersonic.headless.api.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class SchemaMapInfo {

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
