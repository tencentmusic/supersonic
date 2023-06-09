package com.tencent.supersonic.semantic.core.domain.manager;

import com.tencent.supersonic.semantic.api.core.pojo.DatasourceDetail;
import com.tencent.supersonic.semantic.api.core.pojo.Dim;
import com.tencent.supersonic.semantic.api.core.pojo.Identify;
import com.tencent.supersonic.semantic.api.core.pojo.Measure;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DimensionTimeTypeParamsTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.IdentifyYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MeasureYamlTpl;
import com.tencent.supersonic.semantic.api.core.response.DatabaseResp;
import com.tencent.supersonic.semantic.core.domain.utils.SysTimeDimensionBuilder;
import com.tencent.supersonic.semantic.core.domain.adaptor.engineadapter.EngineAdaptor;
import com.tencent.supersonic.semantic.core.domain.adaptor.engineadapter.EngineAdaptorFactory;
import com.tencent.supersonic.semantic.core.domain.pojo.Datasource;
import com.tencent.supersonic.semantic.core.domain.pojo.DatasourceQueryEnum;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class DatasourceYamlManager {

    public static DatasourceYamlTpl convert2YamlObj(Datasource datasource, DatabaseResp databaseResp) {
        DatasourceDetail datasourceDetail = datasource.getDatasourceDetail();
        EngineAdaptor engineAdaptor = EngineAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        SysTimeDimensionBuilder.addSysTimeDimension(datasourceDetail.getDimensions(), engineAdaptor);
        addInterCntMetric(datasource.getBizName(), datasourceDetail.getMeasures());
        DatasourceYamlTpl datasourceYamlTpl = new DatasourceYamlTpl();
        BeanUtils.copyProperties(datasourceDetail, datasourceYamlTpl);
        datasourceYamlTpl.setIdentifiers(datasourceDetail.getIdentifiers().stream().map(DatasourceYamlManager::convert)
                .collect(Collectors.toList()));
        datasourceYamlTpl.setDimensions(datasourceDetail.getDimensions().stream().map(DatasourceYamlManager::convert)
                .collect(Collectors.toList()));
        datasourceYamlTpl.setMeasures(datasourceDetail.getMeasures().stream().map(DatasourceYamlManager::convert)
                .collect(Collectors.toList()));
        datasourceYamlTpl.setName(datasource.getBizName());
        datasourceYamlTpl.setSourceId(datasource.getDatabaseId());
        if (datasourceDetail.getQueryType().equalsIgnoreCase(DatasourceQueryEnum.SQL_QUERY.getName())) {
            datasourceYamlTpl.setSqlQuery(datasourceDetail.getSqlQuery());
        } else {
            datasourceYamlTpl.setTableQuery(datasourceDetail.getTableQuery());
        }
        return datasourceYamlTpl;
    }

    public static DimensionYamlTpl convert(Dim dim) {
        DimensionYamlTpl dimensionYamlTpl = new DimensionYamlTpl();
        BeanUtils.copyProperties(dim, dimensionYamlTpl);
        dimensionYamlTpl.setName(dim.getBizName());
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


    private static void addInterCntMetric(String datasourceEnName, List<Measure> measures) {
        Measure measure = new Measure();
        measure.setExpr("1");
        measure.setAgg("count");
        measure.setBizName(String.format("%s_%s", datasourceEnName, "internal_cnt"));
        measure.setCreateMetric("true");
        measures.add(measure);
    }


}
