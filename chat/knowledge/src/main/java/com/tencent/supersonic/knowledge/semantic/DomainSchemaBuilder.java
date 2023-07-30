package com.tencent.supersonic.knowledge.semantic;

import com.tencent.supersonic.chat.api.pojo.DomainSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class DomainSchemaBuilder {

    public static DomainSchema build(DomainSchemaResp resp) {
        DomainSchema domainSchema = new DomainSchema();

        SchemaElement domain = SchemaElement.builder()
                .domain(resp.getId())
                .id(resp.getId())
                .name(resp.getName())
                .bizName(resp.getBizName())
                .type(SchemaElementType.DOMAIN)
                .build();
        domainSchema.setDomain(domain);

        Set<SchemaElement> metrics = new HashSet<>();
        for (MetricSchemaResp metric : resp.getMetrics()) {
            SchemaElement metricToAdd = SchemaElement.builder()
                    .domain(resp.getId())
                    .id(metric.getId())
                    .name(metric.getName())
                    .bizName(metric.getBizName())
                    .type(SchemaElementType.METRIC)
                    .useCnt(metric.getUseCnt())
                    .build();
            metrics.add(metricToAdd);

            String alias = metric.getAlias();
            if (StringUtils.isNotEmpty(alias)) {
                SchemaElement alisMetricToAdd = new SchemaElement();
                BeanUtils.copyProperties(metricToAdd, alisMetricToAdd);
                alisMetricToAdd.setName(alias);
                metrics.add(alisMetricToAdd);
            }
        }
        domainSchema.getMetrics().addAll(metrics);

        Set<SchemaElement> dimensions = new HashSet<>();
        Set<SchemaElement> dimensionValues = new HashSet<>();
        for (DimSchemaResp dim : resp.getDimensions()) {

            Set<String> dimValueAlias = new HashSet<>();
            if (!CollectionUtils.isEmpty(dim.getDimValueMaps())) {
                List<DimValueMap> dimValueMaps = dim.getDimValueMaps();
                for (DimValueMap dimValueMap : dimValueMaps) {
                    if (Strings.isNotEmpty(dimValueMap.getBizName())) {
                        dimValueAlias.add(dimValueMap.getBizName());
                    }
                    if (!CollectionUtils.isEmpty(dimValueMap.getAlias())) {
                        dimValueAlias.addAll(dimValueMap.getAlias());
                    }
                }
            }

            SchemaElement dimToAdd = SchemaElement.builder()
                    .domain(resp.getId())
                    .id(dim.getId())
                    .name(dim.getName())
                    .bizName(dim.getBizName())
                    .type(SchemaElementType.DIMENSION)
                    .useCnt(dim.getUseCnt())
                    .build();
            dimensions.add(dimToAdd);

            String alias = dim.getAlias();
            if (StringUtils.isNotEmpty(alias)) {
                SchemaElement alisDimToAdd = new SchemaElement();
                BeanUtils.copyProperties(dimToAdd, alisDimToAdd);
                alisDimToAdd.setName(alias);
                dimensions.add(alisDimToAdd);
            }


            SchemaElement dimValueToAdd = SchemaElement.builder()
                    .domain(resp.getId())
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

        if (!CollectionUtils.isEmpty(resp.getEntityNames())) {
            Set<SchemaElement> entities = new HashSet<>();
            for (String entity : resp.getEntityNames()) {
                entities.add(SchemaElement.builder()
                        .domain(resp.getId())
                        .id(resp.getId())
                        .name(entity)
                        .bizName(entity)
                        .type(SchemaElementType.ENTITY)
                        .build());
            }
            domainSchema.getEntities().addAll(entities);
        }

        return domainSchema;
    }
}
