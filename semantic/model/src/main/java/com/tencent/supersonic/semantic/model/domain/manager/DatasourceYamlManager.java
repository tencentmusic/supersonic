package com.tencent.supersonic.semantic.model.domain.manager;

import com.tencent.supersonic.semantic.api.model.pojo.ModelDetail;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.yaml.DataModelYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionTimeTypeParamsTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.IdentifyYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MeasureYamlTpl;
import com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter.EngineAdaptor;
import com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter.EngineAdaptorFactory;
import com.tencent.supersonic.semantic.model.domain.pojo.Datasource;
import com.tencent.supersonic.semantic.model.domain.pojo.DatasourceQueryEnum;
import com.tencent.supersonic.semantic.model.domain.utils.SysTimeDimensionBuilder;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service
@Slf4j
public class DatasourceYamlManager {

    public static DataModelYamlTpl convert2YamlObj(Datasource datasource, DatabaseResp databaseResp) {
        ModelDetail datasourceDetail = datasource.getDatasourceDetail();
        EngineAdaptor engineAdaptor = EngineAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        SysTimeDimensionBuilder.addSysTimeDimension(datasourceDetail.getDimensions(), engineAdaptor);
        addInterCntMetric(datasource.getBizName(), datasourceDetail);
        DataModelYamlTpl dataModelYamlTpl = new DataModelYamlTpl();
        BeanUtils.copyProperties(datasourceDetail, dataModelYamlTpl);
        dataModelYamlTpl.setIdentifiers(datasourceDetail.getIdentifiers().stream().map(DatasourceYamlManager::convert)
                .collect(Collectors.toList()));
        dataModelYamlTpl.setDimensions(datasourceDetail.getDimensions().stream().map(DatasourceYamlManager::convert)
                .collect(Collectors.toList()));
        dataModelYamlTpl.setMeasures(datasourceDetail.getMeasures().stream().map(DatasourceYamlManager::convert)
                .collect(Collectors.toList()));
        dataModelYamlTpl.setName(datasource.getBizName());
        dataModelYamlTpl.setSourceId(datasource.getDatabaseId());
        if (datasourceDetail.getQueryType().equalsIgnoreCase(DatasourceQueryEnum.SQL_QUERY.getName())) {
            dataModelYamlTpl.setSqlQuery(datasourceDetail.getSqlQuery());
        } else {
            dataModelYamlTpl.setTableQuery(datasourceDetail.getTableQuery());
        }
        return dataModelYamlTpl;
    }

    public static DimensionYamlTpl convert(Dim dim) {
        DimensionYamlTpl dimensionYamlTpl = new DimensionYamlTpl();
        BeanUtils.copyProperties(dim, dimensionYamlTpl);
        dimensionYamlTpl.setName(dim.getBizName());
        if (Objects.isNull(dimensionYamlTpl.getExpr())) {
            dimensionYamlTpl.setExpr(dim.getBizName());
        }
        if (dim.getTypeParams() != null) {
            DimensionTimeTypeParamsTpl dimensionTimeTypeParamsTpl = new DimensionTimeTypeParamsTpl();
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

    private static void addInterCntMetric(String datasourceEnName, ModelDetail datasourceDetail) {
        Measure measure = new Measure();
        measure.setExpr("1");
        if (!CollectionUtils.isEmpty(datasourceDetail.getIdentifiers())) {
            measure.setExpr(datasourceDetail.getIdentifiers().get(0).getBizName());
        }
        measure.setAgg("count");
        measure.setBizName(String.format("%s_%s", datasourceEnName, "internal_cnt"));
        measure.setCreateMetric("true");
        datasourceDetail.getMeasures().add(measure);
    }

}
