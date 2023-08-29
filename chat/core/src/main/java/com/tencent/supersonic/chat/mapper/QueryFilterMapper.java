package com.tencent.supersonic.chat.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.common.pojo.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class QueryFilterMapper implements SchemaMapper {

    private Long frequency = 9999999L;
    private double similarity = 1.0;

    @Override
    public void map(QueryContext queryContext) {
        QueryReq queryReq = queryContext.getRequest();
        Long modelId = queryReq.getModelId();
        if (modelId == null || modelId <= 0) {
            return;
        }
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        clearOtherSchemaElementMatch(modelId, schemaMapInfo);
        List<SchemaElementMatch> schemaElementMatches = schemaMapInfo.getMatchedElements(modelId);
        if (schemaElementMatches == null) {
            schemaElementMatches = Lists.newArrayList();
            schemaMapInfo.setMatchedElements(modelId, schemaElementMatches);
        }
        addValueSchemaElementMatch(schemaElementMatches, queryReq.getQueryFilters());
    }

    private void clearOtherSchemaElementMatch(Long modelId, SchemaMapInfo schemaMapInfo) {
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMapInfo.getModelElementMatches().entrySet()) {
            if (!entry.getKey().equals(modelId)) {
                entry.getValue().clear();
            }
        }
    }

    private List<SchemaElementMatch> addValueSchemaElementMatch(List<SchemaElementMatch> candidateElementMatches,
                                                           QueryFilters queryFilter) {
        if (queryFilter == null || CollectionUtils.isEmpty(queryFilter.getFilters())) {
            return candidateElementMatches;
        }
        for (QueryFilter filter : queryFilter.getFilters()) {
            if (checkExistSameValueSchemaElementMatch(filter, candidateElementMatches)) {
                continue;
            }
            SchemaElement element = SchemaElement.builder()
                    .id(filter.getElementID())
                    .name(String.valueOf(filter.getValue()))
                    .type(SchemaElementType.VALUE)
                    .bizName(filter.getBizName())
                    .build();
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .element(element)
                    .frequency(frequency)
                    .word(String.valueOf(filter.getValue()))
                    .similarity(similarity)
                    .detectWord(Constants.EMPTY)
                    .build();
            candidateElementMatches.add(schemaElementMatch);
        }
        return candidateElementMatches;
    }

    private boolean checkExistSameValueSchemaElementMatch(QueryFilter queryFilter,
                                                          List<SchemaElementMatch> schemaElementMatches) {
        List<SchemaElementMatch> valueSchemaElements = schemaElementMatches.stream().filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toList());
        for (SchemaElementMatch schemaElementMatch : valueSchemaElements) {
            if (schemaElementMatch.getElement().getId().equals(queryFilter.getElementID())
                    && schemaElementMatch.getWord().equals(String.valueOf(queryFilter.getValue()))) {
                return true;
            }
        }
        return false;
    }
}
