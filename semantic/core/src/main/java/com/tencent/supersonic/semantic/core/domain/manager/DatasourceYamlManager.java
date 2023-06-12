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
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.common.enums.TypeEnums;
import com.tencent.supersonic.common.util.yaml.YamlUtils;
import com.tencent.supersonic.semantic.core.domain.utils.SysTimeDimensionBuilder;
import com.tencent.supersonic.semantic.core.domain.adaptor.engineadapter.EngineAdaptor;
import com.tencent.supersonic.semantic.core.domain.adaptor.engineadapter.EngineAdaptorFactory;
import com.tencent.supersonic.semantic.core.domain.pojo.Datasource;
import com.tencent.supersonic.semantic.core.domain.pojo.DatasourceQueryEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service
@Slf4j
public class DatasourceYamlManager {


    private YamlManager yamlManager;

    public DatasourceYamlManager(YamlManager yamlManager) {
        this.yamlManager = yamlManager;
    }


    public void generateYamlFile(Datasource datasource, DatabaseResp databaseResp, String fullPath,
            List<DimensionResp> dimensionDescsExist) throws Exception {
        if (!CollectionUtils.isEmpty(dimensionDescsExist)) {
            List<String> dimensionBizNames = dimensionDescsExist.stream().map(DimensionResp::getBizName)
                    .collect(Collectors.toList());
            datasource.getDatasourceDetail().getDimensions()
                    .removeIf(dim -> dimensionBizNames.contains(dim.getBizName()));
        }
        String yamlStr = convert2YamlStr(datasource, databaseResp);
        log.info("generate yaml str :{} from datasource:{} full path:{}", yamlStr, datasource, fullPath);
        yamlManager.generateYamlFile(yamlStr, fullPath, getYamlName(datasource.getBizName()));
    }

    public void deleteYamlFile(String datasourceBizName, String fullPath) throws Exception {
        log.info("delete datasource yaml :{} ,fullPath:{}", datasourceBizName, fullPath);
        yamlManager.deleteYamlFile(fullPath, getYamlName(datasourceBizName));
    }

    public String getYamlName(String name) {
        return String.format("%s_%s", name, TypeEnums.DATASOURCE.getName());
    }

    public static String convert2YamlStr(Datasource datasource, DatabaseResp databaseResp) {
        DatasourceYamlTpl datasourceYamlTpl = convert2YamlObj(datasource, databaseResp);
        Map<String, Object> rootMap = new HashMap<>();
        rootMap.put("data_source", datasourceYamlTpl);
        return YamlUtils.toYamlWithoutNull(rootMap);
    }

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
