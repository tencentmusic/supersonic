package com.tencent.supersonic.headless.api.pojo;

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

    public void setDataSetElementMatches(Map<Long, List<SchemaElementMatch>> dataSetElementMatches) {
        this.dataSetElementMatches = dataSetElementMatches;
    }

    public void setMatchedElements(Long dataSet, List<SchemaElementMatch> elementMatches) {
        dataSetElementMatches.put(dataSet, elementMatches);
    }

    public Map<Long, String> generateDataSetMap() {
        Map<Long, String> dataSetIdToName = new HashMap<>();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : dataSetElementMatches.entrySet()) {
            List<SchemaElementMatch> values = entry.getValue();
            if (CollectionUtils.isNotEmpty(values)) {
                SchemaElementMatch schemaElementMatch = values.get(0);
                String dataSetName = schemaElementMatch.getElement().getDataSetName();
                dataSetIdToName.put(entry.getKey(), dataSetName);
            }
        }
        return dataSetIdToName;
    }

}
