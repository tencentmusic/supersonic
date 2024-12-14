package com.tencent.supersonic.headless.server.manager;

import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.enums.ModelDefineType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.pojo.yaml.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Collectors;

/** manager to handle the model */
@Service
@Slf4j
public class ModelYamlManager {

    public static synchronized DataModelYamlTpl convert2YamlObj(ModelResp modelResp,
            DatabaseResp databaseResp) {
        ModelDetail modelDetail = modelResp.getModelDetail();
        DataModelYamlTpl dataModelYamlTpl = new DataModelYamlTpl();
        dataModelYamlTpl.setType(databaseResp.getType());
        BeanUtils.copyProperties(modelDetail, dataModelYamlTpl);
        dataModelYamlTpl.setIdentifiers(modelDetail.getIdentifiers().stream()
                .map(ModelYamlManager::convert).collect(Collectors.toList()));
        dataModelYamlTpl.setDimensions(modelDetail.getDimensions().stream()
                .map(ModelYamlManager::convert).collect(Collectors.toList()));
        dataModelYamlTpl.setMeasures(modelDetail.getMeasures().stream()
                .map(ModelYamlManager::convert).collect(Collectors.toList()));
        dataModelYamlTpl.setName(modelResp.getBizName());
        dataModelYamlTpl.setSourceId(modelResp.getDatabaseId());
        if (modelDetail.getQueryType().equalsIgnoreCase(ModelDefineType.SQL_QUERY.getName())) {
            dataModelYamlTpl.setSqlQuery(modelDetail.getSqlQuery());
        } else {
            dataModelYamlTpl.setTableQuery(modelDetail.getTableQuery());
        }
        dataModelYamlTpl.setFields(modelResp.getModelDetail().getFields());
        dataModelYamlTpl.setId(modelResp.getId());
        return dataModelYamlTpl;
    }

    public static DimensionYamlTpl convert(Dimension dim) {
        DimensionYamlTpl dimensionYamlTpl = new DimensionYamlTpl();
        BeanUtils.copyProperties(dim, dimensionYamlTpl);
        dimensionYamlTpl.setName(dim.getBizName());
        if (Objects.isNull(dimensionYamlTpl.getExpr())) {
            dimensionYamlTpl.setExpr(dim.getBizName());
        }
        if (dim.getTypeParams() != null) {
            DimensionTimeTypeParams dimensionTimeTypeParamsTpl = new DimensionTimeTypeParams();
            dimensionTimeTypeParamsTpl.setIsPrimary(dim.getTypeParams().getIsPrimary());
            dimensionTimeTypeParamsTpl.setTimeGranularity(dim.getTypeParams().getTimeGranularity());
            dimensionYamlTpl.setTypeParams(dimensionTimeTypeParamsTpl);
        }
        return dimensionYamlTpl;
    }

    public static MeasureYamlTpl convert(Measure measure) {
        MeasureYamlTpl measureYamlTpl = new MeasureYamlTpl();
        BeanUtils.copyProperties(measure, measureYamlTpl);
        measureYamlTpl.setName(measure.getBizName());
        return measureYamlTpl;
    }

    public static IdentifyYamlTpl convert(Identify identify) {
        IdentifyYamlTpl identifyYamlTpl = new IdentifyYamlTpl();
        identifyYamlTpl.setName(identify.getBizName());
        identifyYamlTpl.setType(identify.getType());
        return identifyYamlTpl;
    }

}
