package com.tencent.supersonic.chat.mapper;

import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

@Slf4j
public class QueryFilterMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {
        QueryRequest queryReq = queryContext.getRequest();
        Long domainId = queryReq.getDomainId();
        if (domainId == null || domainId <= 0) {
            return;
        }
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        clearOtherSchemaElementMatch(domainId, schemaMapInfo);
    }

    private void clearOtherSchemaElementMatch(Long domainId,  SchemaMapInfo schemaMapInfo) {
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMapInfo.getDomainElementMatches().entrySet()) {
            if (!entry.getKey().equals(domainId)) {
                entry.getValue().clear();
            }
        }
    }

}
