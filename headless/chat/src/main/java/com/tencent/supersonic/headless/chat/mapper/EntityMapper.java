package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/** A mapper capable of converting the VALUE of entity dimension values into ID types. */
@Slf4j
public class EntityMapper extends BaseMapper {

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        SchemaMapInfo schemaMapInfo = chatQueryContext.getMapInfo();
        for (Long dataSetId : schemaMapInfo.getMatchedDataSetInfos()) {
            List<SchemaElementMatch> schemaElementMatchList =
                    schemaMapInfo.getMatchedElements(dataSetId);
            if (CollectionUtils.isEmpty(schemaElementMatchList)) {
                continue;
            }
            SchemaElement entity = getEntity(dataSetId, chatQueryContext);
            if (entity == null || entity.getId() == null) {
                continue;
            }
            List<SchemaElementMatch> valueSchemaElements = schemaElementMatchList.stream()
                    .filter(schemaElementMatch -> SchemaElementType.VALUE
                            .equals(schemaElementMatch.getElement().getType()))
                    .collect(Collectors.toList());
            for (SchemaElementMatch schemaElementMatch : valueSchemaElements) {
                if (!entity.getId().equals(schemaElementMatch.getElement().getId())) {
                    continue;
                }
                if (!checkExistSameEntitySchemaElements(schemaElementMatch,
                        schemaElementMatchList)) {
                    SchemaElementMatch entitySchemaElementMath = new SchemaElementMatch();
                    BeanUtils.copyProperties(schemaElementMatch, entitySchemaElementMath);
                    entitySchemaElementMath.setElement(entity);
                    schemaElementMatchList.add(entitySchemaElementMath);
                }
                schemaElementMatch.getElement().setType(SchemaElementType.ID);
            }
        }
    }

    private boolean checkExistSameEntitySchemaElements(SchemaElementMatch valueSchemaElementMatch,
            List<SchemaElementMatch> schemaElementMatchList) {
        List<SchemaElementMatch> entitySchemaElements = schemaElementMatchList.stream()
                .filter(schemaElementMatch -> SchemaElementType.ENTITY
                        .equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toList());
        for (SchemaElementMatch schemaElementMatch : entitySchemaElements) {
            if (schemaElementMatch.getElement().getId()
                    .equals(valueSchemaElementMatch.getElement().getId())) {
                return true;
            }
        }
        return false;
    }

    private SchemaElement getEntity(Long dataSetId, ChatQueryContext chatQueryContext) {
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        DataSetSchema modelSchema = semanticSchema.getDataSetSchemaMap().get(dataSetId);
        if (modelSchema != null && modelSchema.getEntity() != null) {
            return modelSchema.getEntity();
        }
        return null;
    }
}
