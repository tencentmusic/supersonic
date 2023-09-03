package com.tencent.supersonic.semantic.model.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.enums.MetricTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.DatasourceDetail;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.request.DatasourceReq;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.response.DatasourceRelaResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceRelaDO;
import com.tencent.supersonic.semantic.model.domain.pojo.Datasource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;


public class DatasourceConverter {


    public static Datasource convert(DatasourceReq datasourceReq) {
        Datasource datasource = new Datasource();
        DatasourceDetail datasourceDetail = new DatasourceDetail();
        BeanUtils.copyProperties(datasourceReq, datasource);
        BeanUtils.copyProperties(datasourceReq, datasourceDetail);
        List<Measure> measures = datasourceDetail.getMeasures();
        for (Measure measure : measures) {
            if (StringUtils.isBlank(measure.getExpr())) {
                measure.setExpr(measure.getBizName());
            }
            if (StringUtils.isBlank(measure.getConstraint())) {
                measure.setConstraint(null);
            }
            if (StringUtils.isBlank(measure.getAlias())) {
                measure.setAlias(null);
            }
            measure.setBizName(String.format("%s_%s", datasource.getBizName(), measure.getBizName()));
        }
        datasource.setStatus(StatusEnum.ONLINE.getCode());
        datasource.setDatasourceDetail(datasourceDetail);
        return datasource;
    }

    public static DatasourceRelaResp convert(DatasourceRelaDO datasourceRelaDO) {
        DatasourceRelaResp datasourceRelaResp = new DatasourceRelaResp();
        BeanUtils.copyProperties(datasourceRelaDO, datasourceRelaResp);
        return datasourceRelaResp;
    }


    public static DatasourceDO convert(DatasourceDO datasourceDO, Datasource datasource) {
        BeanMapper.mapper(datasource, datasourceDO);
        datasourceDO.setDatasourceDetail(JSONObject.toJSONString((datasource.getDatasourceDetail())));
        return datasourceDO;
    }


    public static DatasourceDO convert(Datasource datasource, User user) {
        DatasourceDO datasourceDO = new DatasourceDO();
        BeanUtils.copyProperties(datasource, datasourceDO);
        datasourceDO.setDatasourceDetail(JSONObject.toJSONString(datasource.getDatasourceDetail()));
        datasourceDO.setUpdatedBy(user.getName());
        datasourceDO.setUpdatedAt(new Date());
        datasourceDO.setCreatedBy(user.getName());
        datasourceDO.setCreatedAt(new Date());
        return datasourceDO;
    }


    public static DatasourceResp convert(DatasourceDO datasourceDO) {
        DatasourceResp datasourceDesc = new DatasourceResp();
        BeanUtils.copyProperties(datasourceDO, datasourceDesc);
        datasourceDesc.setDatasourceDetail(
                JSONObject.parseObject(datasourceDO.getDatasourceDetail(), DatasourceDetail.class));
        return datasourceDesc;
    }

    public static MeasureResp convert(Measure measure, DatasourceResp datasourceDesc) {
        MeasureResp measureDesc = new MeasureResp();
        BeanUtils.copyProperties(measure, measureDesc);
        measureDesc.setDatasourceId(datasourceDesc.getId());
        measureDesc.setDatasourceName(datasourceDesc.getName());
        measureDesc.setDatasourceBizName(datasourceDesc.getBizName());
        return measureDesc;
    }

    public static DimensionReq convert(Dim dim, Datasource datasource) {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setName(dim.getName());
        dimensionReq.setBizName(dim.getBizName());
        dimensionReq.setDescription(dim.getName());
        dimensionReq.setSemanticType("CATEGORY");
        dimensionReq.setDatasourceId(datasource.getId());
        dimensionReq.setModelId(datasource.getModelId());
        dimensionReq.setExpr(dim.getBizName());
        dimensionReq.setType("categorical");
        return dimensionReq;
    }

    public static MetricReq convert(Measure measure, Datasource datasource) {
        measure.setDatasourceId(datasource.getId());
        MetricReq metricReq = new MetricReq();
        metricReq.setName(measure.getName());
        metricReq.setBizName(measure.getBizName().replaceFirst(datasource.getBizName() + "_", ""));
        metricReq.setDescription(measure.getName());
        metricReq.setModelId(datasource.getModelId());
        metricReq.setMetricType(MetricTypeEnum.ATOMIC);
        MetricTypeParams exprTypeParams = new MetricTypeParams();
        exprTypeParams.setExpr(measure.getBizName());
        exprTypeParams.setMeasures(Lists.newArrayList(measure));
        metricReq.setTypeParams(exprTypeParams);
        return metricReq;
    }

    public static DimensionReq convert(Identify identify, Datasource datasource) {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setName(identify.getName());
        dimensionReq.setBizName(identify.getBizName());
        dimensionReq.setDescription(identify.getName());
        dimensionReq.setSemanticType("CATEGORY");
        dimensionReq.setDatasourceId(datasource.getId());
        dimensionReq.setModelId(datasource.getModelId());
        dimensionReq.setExpr(identify.getBizName());
        dimensionReq.setType(identify.getType());
        return dimensionReq;
    }

    public static List<DatasourceResp> convertList(List<DatasourceDO> datasourceDOS) {
        List<DatasourceResp> datasourceDescs = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(datasourceDOS)) {
            datasourceDescs = datasourceDOS.stream().map(DatasourceConverter::convert).collect(Collectors.toList());
        }
        return datasourceDescs;
    }


    private static boolean isCraeteDimension(Dim dim) {
        return dim.getIsCreateDimension() == 1
                && StringUtils.isNotBlank(dim.getName())
                && !dim.getType().equalsIgnoreCase("time");
    }

    private static boolean isCraeteMetric(Measure measure) {
        return measure.getIsCreateMetric() == 1
                && StringUtils.isNotBlank(measure.getName());
    }

    public static List<Dim> getDimToCreateDimension(Datasource datasource) {
        return datasource.getDatasourceDetail().getDimensions().stream()
                .filter(DatasourceConverter::isCraeteDimension)
                .collect(Collectors.toList());
    }

    public static List<Measure> getMeasureToCreateMetric(Datasource datasource) {
        return datasource.getDatasourceDetail().getMeasures().stream()
                .filter(DatasourceConverter::isCraeteMetric)
                .collect(Collectors.toList());
    }

    public static List<DimensionReq> convertDimensionList(Datasource datasource) {
        List<DimensionReq> dimensionReqs = Lists.newArrayList();
        List<Dim> dims = getDimToCreateDimension(datasource);
        if (!CollectionUtils.isEmpty(dims)) {
            dimensionReqs = dims.stream().filter(dim -> StringUtils.isNotBlank(dim.getName()))
                    .map(dim -> convert(dim, datasource)).collect(Collectors.toList());
        }
        List<Identify> identifies = datasource.getDatasourceDetail().getIdentifiers();
        if (CollectionUtils.isEmpty(identifies)) {
            return dimensionReqs;
        }
        dimensionReqs.addAll(identifies.stream()
                .filter(i -> i.getType().equalsIgnoreCase("primary"))
                .filter(i -> StringUtils.isNotBlank(i.getName()))
                .map(identify -> convert(identify, datasource)).collect(Collectors.toList()));
        return dimensionReqs;
    }


    public static List<MetricReq> convertMetricList(Datasource datasource) {
        List<Measure> measures = getMeasureToCreateMetric(datasource);
        if (CollectionUtils.isEmpty(measures)) {
            return Lists.newArrayList();
        }
        return measures.stream().map(measure -> convert(measure, datasource)).collect(Collectors.toList());
    }

    public static Datasource datasourceInfo2Datasource(DatasourceResp datasourceResp) {
        Datasource datasource = new Datasource();
        BeanUtils.copyProperties(datasourceResp, datasource);
        return datasource;
    }
}
