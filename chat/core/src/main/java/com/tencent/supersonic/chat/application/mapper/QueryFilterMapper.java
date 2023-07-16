package com.tencent.supersonic.chat.application.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.common.constant.Constants;
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
    public void map(QueryContextReq queryContext) {
        Integer domainId = queryContext.getDomainId();
        if (domainId == null || domainId <= 0 || queryContext.getQueryFilter() == null) {
            return;
        }
        QueryFilter queryFilter = queryContext.getQueryFilter();
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        List<SchemaElementMatch> schemaElementMatches = schemaMapInfo.getMatchedElements(domainId);
        clearOtherSchemaElementMatch(domainId, schemaMapInfo);
        convertFilterToSchemaMapInfo(queryFilter.getFilters(), schemaElementMatches);
    }

    private void convertFilterToSchemaMapInfo(List<Filter> filters, List<SchemaElementMatch> schemaElementMatches) {
        log.info("schemaElementMatches before queryFilerMapper:{}", schemaElementMatches);
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            schemaElementMatches = Lists.newArrayList();
        }
        List<String> words = schemaElementMatches.stream().map(SchemaElementMatch::getWord).collect(Collectors.toList());
        for (Filter filter : filters) {
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .elementType(SchemaElementType.VALUE)
                    .elementID(filter.getElementID().intValue())
                    .frequency(FREQUENCY)
                    .word(String.valueOf(filter.getValue()))
                    .similarity(SIMILARITY)
                    .detectWord(Constants.EMPTY)
                    .build();
            if (words.contains(schemaElementMatch.getWord())) {
                continue;
            }
            schemaElementMatches.add(schemaElementMatch);
        }
        log.info("schemaElementMatches after queryFilerMapper:{}", schemaElementMatches);
    }

    private void clearOtherSchemaElementMatch(Integer domainId,  SchemaMapInfo schemaMapInfo) {
        for (Map.Entry<Integer, List<SchemaElementMatch>> entry : schemaMapInfo.getDomainElementMatches().entrySet()) {
            if (!entry.getKey().equals(domainId)) {
                entry.getValue().clear();
            }
        }
    }

}
