package com.tencent.supersonic.headless.chat.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.builder.BaseWordBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class QueryFilterMapper extends BaseMapper {

    private final double similarity = 1.0;

    @Override
    public boolean accept(ChatQueryContext chatQueryContext) {
        return !chatQueryContext.getRequest().getDataSetIds().isEmpty();
    }

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        Set<Long> dataSetIds = chatQueryContext.getRequest().getDataSetIds();
        SchemaMapInfo schemaMapInfo = chatQueryContext.getMapInfo();
        clearOtherSchemaElementMatch(dataSetIds, schemaMapInfo);
        for (Long dataSetId : dataSetIds) {
            List<SchemaElementMatch> schemaElementMatches =
                    schemaMapInfo.getMatchedElements(dataSetId);
            if (schemaElementMatches == null) {
                schemaElementMatches = Lists.newArrayList();
                schemaMapInfo.setMatchedElements(dataSetId, schemaElementMatches);
            }
            addValueSchemaElementMatch(dataSetId, chatQueryContext, schemaElementMatches);
        }
    }

    private void clearOtherSchemaElementMatch(Set<Long> viewIds, SchemaMapInfo schemaMapInfo) {
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMapInfo
                .getDataSetElementMatches().entrySet()) {
            if (!viewIds.contains(entry.getKey())) {
                entry.getValue().clear();
            }
        }
    }

    private void addValueSchemaElementMatch(Long dataSetId, ChatQueryContext chatQueryContext,
            List<SchemaElementMatch> candidateElementMatches) {
        QueryFilters queryFilters = chatQueryContext.getRequest().getQueryFilters();
        if (queryFilters == null || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return;
        }
        for (QueryFilter filter : queryFilters.getFilters()) {
            if (checkExistSameValueSchemaElementMatch(filter, candidateElementMatches)) {
                continue;
            }
            SchemaElement element = SchemaElement.builder().id(filter.getElementID())
                    .name(String.valueOf(filter.getValue())).type(SchemaElementType.VALUE)
                    .bizName(filter.getBizName()).dataSetId(dataSetId).build();
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder().element(element)
                    .frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                    .word(String.valueOf(filter.getValue())).similarity(similarity)
                    .detectWord(Constants.EMPTY).build();
            candidateElementMatches.add(schemaElementMatch);
        }
        chatQueryContext.getMapInfo().setMatchedElements(dataSetId, candidateElementMatches);
    }

    private boolean checkExistSameValueSchemaElementMatch(QueryFilter queryFilter,
            List<SchemaElementMatch> schemaElementMatches) {
        List<SchemaElementMatch> valueSchemaElements = schemaElementMatches.stream()
                .filter(schemaElementMatch -> SchemaElementType.VALUE
                        .equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toList());
        for (SchemaElementMatch schemaElementMatch : valueSchemaElements) {
            if (schemaElementMatch.getElement().getId().equals(queryFilter.getElementID())
                    && schemaElementMatch.getWord()
                            .equals(String.valueOf(queryFilter.getValue()))) {
                return true;
            }
        }
        return false;
    }
}
