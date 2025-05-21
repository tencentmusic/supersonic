package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.Map.Entry;

import static com.tencent.supersonic.headless.chat.parser.llm.TextSimilarityCalculation.getDataSetSimilarity;

/**
 * HeuristicDataSetResolver select ONE most suitable data set out of data sets. The
 * selection is based on the cosine similarity directly between the question text and the dataset name
 */
@Slf4j
public class HeuristicDataSetResolver implements DataSetResolver {

    public Long resolve(ChatQueryContext chatQueryContext, Set<Long> agentDataSetIds) {
        String  queryText = chatQueryContext.getRequest().getQueryText();
        List<SchemaElement> dataSets = chatQueryContext.getSemanticSchema().getDataSets();
        if(dataSets.size() == 1){
            return dataSets.get(0).getDataSetId();
        }
        Map<Long,Double> dataSetSimilarity = new LinkedHashMap<>();
        for (SchemaElement dataSet : dataSets){
            dataSetSimilarity.put(dataSet.getDataSetId(),getDataSetSimilarity(queryText,dataSet.getDataSetName()));
        }
        return dataSetSimilarity.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }
}
