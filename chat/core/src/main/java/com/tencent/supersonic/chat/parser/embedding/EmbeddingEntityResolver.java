package com.tencent.supersonic.chat.parser.embedding;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.service.ConfigService;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component("EmbeddingEntityResolver")
public class EmbeddingEntityResolver {

    private ConfigService configService;

    public EmbeddingEntityResolver(ConfigService configService) {
        this.configService = configService;
    }


    private Long getEntityValue(Long modelId, Long entityElementId, QueryContext queryCtx, ChatContext chatCtx) {
        Long entityId = null;
        QueryFilters queryFilters = queryCtx.getRequest().getQueryFilters();
        if (queryFilters != null) {
            entityId = getEntityValueFromQueryFilter(queryFilters.getFilters());
            if (entityId != null) {
                log.info("get entity id:{} model id:{} from  query filter :{} ", entityId, modelId, queryFilters);
                return entityId;
            }
        }
        entityId = getEntityValueFromSchemaMapInfo(modelId, queryCtx.getMapInfo(), entityElementId);
        log.info("get entity id:{} from  schema map Info :{} ", entityId,
                JSONObject.toJSONString(queryCtx.getMapInfo()));
        if (entityId == null || entityId == 0) {
            Long entityIdFromChat = getEntityValueFromParseInfo(chatCtx.getParseInfo(), entityElementId);
            if (entityIdFromChat != null && entityIdFromChat > 0) {
                entityId = entityIdFromChat;
            }
        }
        return entityId;
    }

    private Long getEntityValueFromQueryFilter(List<QueryFilter> queryFilters) {
        if (CollectionUtils.isEmpty(queryFilters)) {
            return null;
        }
        QueryFilter filter = queryFilters.get(0);
        String value = String.valueOf(filter.getValue());
        if (StringUtils.isNumeric(value)) {
            return Long.parseLong(value);
        }
        return null;
    }

    private Long getEntityValueFromParseInfo(SemanticParseInfo semanticParseInfo, Long entityElementId) {
        Set<QueryFilter> filters = semanticParseInfo.getDimensionFilters();
        if (CollectionUtils.isEmpty(filters)) {
            return null;
        }
        for (QueryFilter filter : filters) {
            if (entityElementId.equals(filter.getElementID())) {
                String value = String.valueOf(filter.getValue());
                if (StringUtils.isNumeric(value)) {
                    return Long.parseLong(value);
                }
            }
        }
        return null;
    }


    private Long getEntityValueFromSchemaMapInfo(Long modelId, SchemaMapInfo schemaMapInfo, Long entityElementId) {
        List<SchemaElementMatch> schemaElementMatchList = schemaMapInfo.getMatchedElements(modelId);
        if (CollectionUtils.isEmpty(schemaElementMatchList)) {
            return null;
        }
        for (SchemaElementMatch schemaElementMatch : schemaElementMatchList) {
            if (Objects.equals(schemaElementMatch.getElement().getId(), entityElementId)) {
                if (StringUtils.isNumeric(schemaElementMatch.getWord())) {
                    return Long.parseLong(schemaElementMatch.getWord());
                }
            }

        }
        return null;
    }

}