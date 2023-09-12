package com.tencent.supersonic.knowledge.semantic;

import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaValueMap;
import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.pojo.Entity;
import com.tencent.supersonic.semantic.api.model.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelSchemaBuilder {

    private static String aliasSplit = ",";

    public static ModelSchema build(ModelSchemaResp resp) {
        ModelSchema domainSchema = new ModelSchema();

        SchemaElement domain = SchemaElement.builder()
                .model(resp.getId())
                .id(resp.getId())
                .name(resp.getName())
                .bizName(resp.getBizName())
                .type(SchemaElementType.MODEL)
                .build();
        domainSchema.setModel(domain);

        Set<SchemaElement> metrics = new HashSet<>();
        for (MetricSchemaResp metric : resp.getMetrics()) {

            List<String> alias = new ArrayList<>();
            String aliasStr = metric.getAlias();
            if (Strings.isNotEmpty(aliasStr)) {
                alias = Arrays.asList(aliasStr.split(aliasSplit));
            }

            SchemaElement metricToAdd = SchemaElement.builder()
                    .model(resp.getId())
                    .id(metric.getId())
                    .name(metric.getName())
                    .bizName(metric.getBizName())
                    .type(SchemaElementType.METRIC)
                    .useCnt(metric.getUseCnt())
                    .alias(alias)
                    .build();
            metrics.add(metricToAdd);

        }
        domainSchema.getMetrics().addAll(metrics);

        Set<SchemaElement> dimensions = new HashSet<>();
        Set<SchemaElement> dimensionValues = new HashSet<>();
        for (DimSchemaResp dim : resp.getDimensions()) {

            List<String> alias = new ArrayList<>();
            String aliasStr = dim.getAlias();
            if (Strings.isNotEmpty(aliasStr)) {
                alias = Arrays.asList(aliasStr.split(aliasSplit));
            }
            Set<String> dimValueAlias = new HashSet<>();
            List<DimValueMap> dimValueMaps = dim.getDimValueMaps();
            List<SchemaValueMap> schemaValueMaps = new ArrayList<>();
            if (!CollectionUtils.isEmpty(dimValueMaps)) {

                for (DimValueMap dimValueMap : dimValueMaps) {
                    if (Strings.isNotEmpty(dimValueMap.getBizName())) {
                        dimValueAlias.add(dimValueMap.getBizName());
                    }
                    if (!CollectionUtils.isEmpty(dimValueMap.getAlias())) {
                        dimValueAlias.addAll(dimValueMap.getAlias());
                    }
                    SchemaValueMap schemaValueMap = new SchemaValueMap();
                    BeanUtils.copyProperties(dimValueMap, schemaValueMap);
                    schemaValueMaps.add(schemaValueMap);
                }

            }
            SchemaElement dimToAdd = SchemaElement.builder()
                    .model(resp.getId())
                    .id(dim.getId())
                    .name(dim.getName())
                    .bizName(dim.getBizName())
                    .type(SchemaElementType.DIMENSION)
                    .useCnt(dim.getUseCnt())
                    .alias(alias)
                    .schemaValueMaps(schemaValueMaps)
                    .build();
            dimensions.add(dimToAdd);

            SchemaElement dimValueToAdd = SchemaElement.builder()
                    .model(resp.getId())
                    .id(dim.getId())
                    .name(dim.getName())
                    .bizName(dim.getBizName())
                    .type(SchemaElementType.VALUE)
                    .useCnt(dim.getUseCnt())
                    .alias(new ArrayList<>(Arrays.asList(dimValueAlias.toArray(new String[0]))))
                    .build();
            dimensionValues.add(dimValueToAdd);
        }
        domainSchema.getDimensions().addAll(dimensions);
        domainSchema.getDimensionValues().addAll(dimensionValues);

        Entity entity = resp.getEntity();
        if (Objects.nonNull(entity)) {
            SchemaElement entityElement = new SchemaElement();

            if (!CollectionUtils.isEmpty(entity.getNames()) && Objects.nonNull(entity.getEntityId())) {
                Map<Long, SchemaElement> idAndDimPair = dimensions.stream()
                        .collect(
                                Collectors.toMap(SchemaElement::getId, schemaElement -> schemaElement, (k1, k2) -> k2));
                if (idAndDimPair.containsKey(entity.getEntityId())) {
                    BeanUtils.copyProperties(idAndDimPair.get(entity.getEntityId()), entityElement);
                    entityElement.setType(SchemaElementType.ENTITY);
                }
                entityElement.setAlias(entity.getNames());
                domainSchema.setEntity(entityElement);
            }
        }

        return domainSchema;
    }
}
