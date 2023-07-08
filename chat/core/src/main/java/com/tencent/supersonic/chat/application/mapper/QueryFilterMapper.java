package com.tencent.supersonic.chat.application.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import java.util.List;


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
        convertFilterToSchemaMapInfo(queryFilter.getFilters(), schemaElementMatches);
    }

    private void convertFilterToSchemaMapInfo(List<Filter> filters, List<SchemaElementMatch> schemaElementMatches) {
        log.info("schemaElementMatches before queryFilerMapper:{}", schemaElementMatches);
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            schemaElementMatches = Lists.newArrayList();
        }
        for (Filter filter : filters) {
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .elementType(SchemaElementType.VALUE)
                    .elementID(filter.getElementID().intValue())
                    .frequency(FREQUENCY)
                    .word(String.valueOf(filter.getValue()))
                    .similarity(SIMILARITY)
                    .detectWord(filter.getName())
                    .build();
            schemaElementMatches.add(schemaElementMatch);
        }
        log.info("schemaElementMatches after queryFilerMapper:{}", schemaElementMatches);
    }

}
