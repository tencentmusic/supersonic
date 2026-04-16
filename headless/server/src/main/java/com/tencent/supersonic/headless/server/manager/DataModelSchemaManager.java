package com.tencent.supersonic.headless.server.manager;

import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.enums.ModelDefineType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.schema.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Collectors;

/** manager to handle the model */
@Service
@Slf4j
public class DataModelSchemaManager {

    public static synchronized DataModelSchema convertToSchema(ModelResp modelResp,
            DatabaseResp databaseResp) {
        ModelDetail modelDetail = modelResp.getModelDetail();
        DataModelSchema dataModelSchema = new DataModelSchema();
        dataModelSchema.setType(databaseResp.getType());
        BeanUtils.copyProperties(modelDetail, dataModelSchema);
        dataModelSchema.setIdentifiers(modelDetail.getIdentifiers().stream()
                .map(DataModelSchemaManager::convert).collect(Collectors.toList()));
        dataModelSchema.setDimensions(modelDetail.getDimensions().stream()
                .map(DataModelSchemaManager::convert).collect(Collectors.toList()));
        dataModelSchema.setMeasures(modelDetail.getMeasures().stream()
                .map(DataModelSchemaManager::convert).collect(Collectors.toList()));
        dataModelSchema.setName(modelResp.getBizName());
        dataModelSchema.setSourceId(modelResp.getDatabaseId());
        if (modelDetail.getQueryType().equalsIgnoreCase(ModelDefineType.SQL_QUERY.getName())) {
            dataModelSchema.setSqlQuery(modelDetail.getSqlQuery());
        } else {
            dataModelSchema.setTableQuery(modelDetail.getTableQuery());
        }
        dataModelSchema.setFilterSql(modelDetail.getFilterSql());
        dataModelSchema.setFields(modelResp.getModelDetail().getFields());
        dataModelSchema.setId(modelResp.getId());
        dataModelSchema.setSqlVariables(modelDetail.getSqlVariables());
        return dataModelSchema;
    }

    public static DimensionSchema convert(Dimension dim) {
        DimensionSchema dimensionSchema = new DimensionSchema();
        BeanUtils.copyProperties(dim, dimensionSchema);
        dimensionSchema.setName(dim.getBizName());
        if (Objects.isNull(dimensionSchema.getExpr())) {
            dimensionSchema.setExpr(dim.getBizName());
        }
        if (dim.getTypeParams() != null) {
            DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams();
            dimensionTimeTypeParams.setIsPrimary(dim.getTypeParams().getIsPrimary());
            dimensionTimeTypeParams.setTimeGranularity(dim.getTypeParams().getTimeGranularity());
            dimensionSchema.setTypeParams(dimensionTimeTypeParams);
        }
        return dimensionSchema;
    }

    public static MeasureSchema convert(Measure measure) {
        MeasureSchema measureSchema = new MeasureSchema();
        BeanUtils.copyProperties(measure, measureSchema);
        measureSchema.setName(measure.getBizName());
        return measureSchema;
    }

    public static IdentifierSchema convert(Identify identify) {
        IdentifierSchema identifierSchema = new IdentifierSchema();
        identifierSchema.setName(identify.getBizName());
        identifierSchema.setType(identify.getType());
        return identifierSchema;
    }

}
