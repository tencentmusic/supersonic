package com.tencent.supersonic.chat.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class QueryFilterMapper implements SchemaMapper {

    private Long FREQUENCY = 9999999L;
    private double SIMILARITY = 1.0;

    @Override
    public void map(QueryContext queryContext) {
        QueryReq queryReq = queryContext.getRequest();
        Long domainId = queryReq.getDomainId();
        if (domainId == null || domainId <= 0) {
            return;
        }
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        clearOtherSchemaElementMatch(domainId, schemaMapInfo);
        List<SchemaElementMatch> schemaElementMatches = schemaMapInfo.getMatchedElements(domainId);
        if (schemaElementMatches == null) {
            schemaElementMatches = Lists.newArrayList();
            schemaMapInfo.setMatchedElements(domainId, schemaElementMatches);
        }
        addValueSchemaElementMatch(schemaElementMatches, queryReq.getQueryFilters());
    }

    private void clearOtherSchemaElementMatch(Long domainId, SchemaMapInfo schemaMapInfo) {
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMapInfo.getDomainElementMatches().entrySet()) {
            if (!entry.getKey().equals(domainId)) {
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
                    .frequency(FREQUENCY)
                    .word(String.valueOf(filter.getValue()))
                    .similarity(SIMILARITY)
                    .detectWord(filter.getName())
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
