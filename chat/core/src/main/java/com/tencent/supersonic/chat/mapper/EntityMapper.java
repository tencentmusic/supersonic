package com.tencent.supersonic.chat.mapper;


import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class EntityMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        for (Long domainId : schemaMapInfo.getMatchedDomains()) {
            List<SchemaElementMatch> schemaElementMatchList = schemaMapInfo.getMatchedElements(domainId);
            if (CollectionUtils.isEmpty(schemaElementMatchList)) {
                continue;
            }
            SchemaElement entity = getEntity(domainId);
            if (entity == null || entity.getId() == null) {
                continue;
            }
            List<SchemaElementMatch> valueSchemaElements = schemaElementMatchList.stream().filter(schemaElementMatch ->
                    SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType()))
                    .collect(Collectors.toList());
            for (SchemaElementMatch schemaElementMatch : valueSchemaElements) {
                if (!entity.getId().equals(schemaElementMatch.getElement().getId())){
                    continue;
                }
                if (!checkExistSameEntitySchemaElements(schemaElementMatch, schemaElementMatchList)) {
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
        List<SchemaElementMatch> entitySchemaElements = schemaElementMatchList.stream().filter(schemaElementMatch ->
                        SchemaElementType.ENTITY.equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toList());
        for (SchemaElementMatch schemaElementMatch : entitySchemaElements) {
            if (schemaElementMatch.getElement().getId().equals(valueSchemaElementMatch.getElement().getId())) {
                return true;
            }
        }
        return false;
    }

    private SchemaElement getEntity(Long domainId) {
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        DomainSchema domainSchema = semanticService.getDomainSchema(domainId);
        if (domainSchema != null && domainSchema.getEntity() != null) {
            return domainSchema.getEntity();
        }
        return null;
    }
}
