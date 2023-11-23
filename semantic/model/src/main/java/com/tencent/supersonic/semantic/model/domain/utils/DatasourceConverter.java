package com.tencent.supersonic.semantic.model.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.semantic.api.model.enums.MetricTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.DatasourceDetail;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.request.DatasourceReq;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.model.domain.pojo.Datasource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class DatasourceConverter {


    public static DatasourceDO convert(DatasourceReq datasourceReq, User user) {
        DatasourceDO datasource = new DatasourceDO();
        DatasourceDetail datasourceDetail = getDatasourceDetail(datasourceReq);
        datasourceReq.createdBy(user.getName());
        BeanMapper.mapper(datasourceReq, datasource);
        datasource.setStatus(StatusEnum.ONLINE.getCode());
        datasource.setDatasourceDetail(JSONObject.toJSONString(datasourceDetail));
        return datasource;
    }


    public static DatasourceDO convert(DatasourceDO datasourceDO, DatasourceReq datasourceReq, User user) {
        DatasourceDetail datasourceDetail = getDatasourceDetail(datasourceReq);
        BeanMapper.mapper(datasourceReq, datasourceDO);
        datasourceDO.setDatasourceDetail(JSONObject.toJSONString((datasourceDetail)));
        datasourceDO.setUpdatedBy(user.getName());
        datasourceDO.setUpdatedAt(new Date());
        return datasourceDO;
    }

    public static DatasourceResp convert(DatasourceDO datasourceDO) {
        DatasourceResp datasourceResp = new DatasourceResp();
        BeanUtils.copyProperties(datasourceDO, datasourceResp);
        datasourceResp.setDatasourceDetail(
                JSONObject.parseObject(datasourceDO.getDatasourceDetail(), DatasourceDetail.class));
        return datasourceResp;
    }

    public static MeasureResp convert(Measure measure, DatasourceResp datasourceResp) {
        MeasureResp measureResp = new MeasureResp();
        BeanUtils.copyProperties(measure, measureResp);
        measureResp.setDatasourceId(datasourceResp.getId());
        measureResp.setDatasourceName(datasourceResp.getName());
        measureResp.setDatasourceBizName(datasourceResp.getBizName());
        measureResp.setModelId(datasourceResp.getModelId());
        return measureResp;
    }

    public static DimensionReq convert(Dim dim, DatasourceDO datasourceDO) {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setName(dim.getName());
        dimensionReq.setBizName(dim.getBizName());
        dimensionReq.setDescription(dim.getName());
        dimensionReq.setSemanticType("CATEGORY");
        dimensionReq.setDatasourceId(datasourceDO.getId());
        dimensionReq.setModelId(datasourceDO.getModelId());
        dimensionReq.setExpr(dim.getBizName());
        dimensionReq.setType("categorical");
        dimensionReq.setDescription(Objects.isNull(dim.getDescription()) ? "" : dim.getDescription());
        dimensionReq.setIsTag(dim.getIsTag());
        return dimensionReq;
    }

    public static MetricReq convert(Measure measure, DatasourceDO datasourceDO) {
        measure.setDatasourceId(datasourceDO.getId());
        MetricReq metricReq = new MetricReq();
        metricReq.setName(measure.getName());
        metricReq.setBizName(measure.getBizName().replaceFirst(datasourceDO.getBizName() + "_", ""));
        metricReq.setDescription(measure.getName());
        metricReq.setModelId(datasourceDO.getModelId());
        metricReq.setMetricType(MetricTypeEnum.ATOMIC);
        MetricTypeParams exprTypeParams = new MetricTypeParams();
        exprTypeParams.setExpr(measure.getBizName());
        exprTypeParams.setMeasures(Lists.newArrayList(measure));
        metricReq.setTypeParams(exprTypeParams);
        return metricReq;
    }

    public static DimensionReq convert(Identify identify, DatasourceDO datasourceDO) {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setName(identify.getName());
        dimensionReq.setBizName(identify.getBizName());
        dimensionReq.setDescription(identify.getName());
        dimensionReq.setSemanticType("CATEGORY");
        dimensionReq.setDatasourceId(datasourceDO.getId());
        dimensionReq.setModelId(datasourceDO.getModelId());
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


    private static boolean isCreateDimension(Dim dim) {
        return dim.getIsCreateDimension() == 1
                && StringUtils.isNotBlank(dim.getName())
                && !dim.getType().equalsIgnoreCase("time");
    }

    private static boolean isCreateMetric(Measure measure) {
        return measure.getIsCreateMetric() == 1
                && StringUtils.isNotBlank(measure.getName());
    }

    public static List<Dim> getDimToCreateDimension(DatasourceDetail datasourceDetail) {
        return datasourceDetail.getDimensions().stream()
                .filter(DatasourceConverter::isCreateDimension)
                .collect(Collectors.toList());
    }

    public static List<Measure> getMeasureToCreateMetric(DatasourceDetail datasourceDetail) {
        return datasourceDetail.getMeasures().stream()
                .filter(DatasourceConverter::isCreateMetric)
                .collect(Collectors.toList());
    }

    public static List<DimensionReq> convertDimensionList(DatasourceDO datasourceDO) {
        List<DimensionReq> dimensionReqs = Lists.newArrayList();
        DatasourceDetail datasourceDetail = JSONObject.parseObject(datasourceDO.getDatasourceDetail(),
                DatasourceDetail.class);
        List<Dim> dims = getDimToCreateDimension(datasourceDetail);
        if (!CollectionUtils.isEmpty(dims)) {
            dimensionReqs = dims.stream().filter(dim -> StringUtils.isNotBlank(dim.getName()))
                    .map(dim -> convert(dim, datasourceDO)).collect(Collectors.toList());
        }
        List<Identify> identifies = datasourceDetail.getIdentifiers();
        if (CollectionUtils.isEmpty(identifies)) {
            return dimensionReqs;
        }
        dimensionReqs.addAll(identifies.stream()
                .filter(i -> i.getType().equalsIgnoreCase("primary"))
                .filter(i -> StringUtils.isNotBlank(i.getName()))
                .map(identify -> convert(identify, datasourceDO)).collect(Collectors.toList()));
        return dimensionReqs;
    }


    public static List<MetricReq> convertMetricList(DatasourceDO datasourceDO) {
        DatasourceDetail datasourceDetail = JSONObject.parseObject(datasourceDO.getDatasourceDetail(),
                DatasourceDetail.class);
        List<Measure> measures = getMeasureToCreateMetric(datasourceDetail);
        if (CollectionUtils.isEmpty(measures)) {
            return Lists.newArrayList();
        }
        return measures.stream().map(measure -> convert(measure, datasourceDO)).collect(Collectors.toList());
    }

    public static Datasource datasourceInfo2Datasource(DatasourceResp datasourceResp) {
        Datasource datasource = new Datasource();
        BeanUtils.copyProperties(datasourceResp, datasource);
        return datasource;
    }

    private static DatasourceDetail getDatasourceDetail(DatasourceReq datasourceReq) {
        DatasourceDetail datasourceDetail = new DatasourceDetail();
        BeanMapper.mapper(datasourceReq, datasourceDetail);
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
            measure.setBizName(String.format("%s_%s", datasourceReq.getBizName(), measure.getBizName()));
        }
        return datasourceDetail;
    }
}
