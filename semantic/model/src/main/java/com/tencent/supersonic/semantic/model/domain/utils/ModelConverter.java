package com.tencent.supersonic.semantic.model.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.enums.MetricTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.ModelDetail;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public class ModelConverter {

    public static ModelDO convert(ModelReq modelReq, User user) {
        ModelDO modelDO = new ModelDO();
        ModelDetail modelDetail = getModelDetail(modelReq);
        modelReq.createdBy(user.getName());
        BeanMapper.mapper(modelReq, modelDO);
        modelDO.setStatus(StatusEnum.ONLINE.getCode());
        modelDO.setModelDetail(JSONObject.toJSONString(modelDetail));
        modelDO.setDrillDownDimensions(JSONObject.toJSONString(modelReq.getDrillDownDimensions()));
        return modelDO;
    }

    public static ModelResp convert(ModelDO modelDO) {
        ModelResp modelResp = new ModelResp();
        BeanUtils.copyProperties(modelDO, modelResp);
        modelResp.setAdmins(StringUtils.isBlank(modelDO.getAdmin())
                ? Lists.newArrayList() : Arrays.asList(modelDO.getAdmin().split(",")));
        modelResp.setAdminOrgs(StringUtils.isBlank(modelDO.getAdminOrg())
                ? Lists.newArrayList() : Arrays.asList(modelDO.getAdminOrg().split(",")));
        modelResp.setViewers(StringUtils.isBlank(modelDO.getViewer())
                ? Lists.newArrayList() : Arrays.asList(modelDO.getViewer().split(",")));
        modelResp.setViewOrgs(StringUtils.isBlank(modelDO.getViewOrg())
                ? Lists.newArrayList() : Arrays.asList(modelDO.getViewOrg().split(",")));
        modelResp.setDrillDownDimensions(JsonUtil.toList(modelDO.getDrillDownDimensions(), DrillDownDimension.class));
        modelResp.setModelDetail(JSONObject.parseObject(modelDO.getModelDetail(), ModelDetail.class));
        return modelResp;
    }

    public static ModelResp convert(ModelDO modelDO, Map<Long, DomainResp> domainRespMap) {
        ModelResp modelResp = convert(modelDO);
        DomainResp domainResp = domainRespMap.get(modelResp.getDomainId());
        if (domainResp != null) {
            String fullBizNamePath = domainResp.getFullPath() + modelResp.getBizName();
            modelResp.setFullPath(fullBizNamePath);
        }
        return modelResp;
    }

    public static ModelDO convert(ModelDO modelDO, ModelReq modelReq, User user) {
        ModelDetail modelDetail = getModelDetail(modelReq);
        BeanMapper.mapper(modelReq, modelDO);
        modelDO.setModelDetail(JSONObject.toJSONString((modelDetail)));
        modelDO.setUpdatedBy(user.getName());
        modelDO.setUpdatedAt(new Date());
        return modelDO;
    }

    public static MeasureResp convert(Measure measure, ModelResp modelResp) {
        MeasureResp measureResp = new MeasureResp();
        BeanUtils.copyProperties(measure, measureResp);
        measureResp.setDatasourceId(modelResp.getId());
        measureResp.setDatasourceName(modelResp.getName());
        measureResp.setDatasourceBizName(modelResp.getBizName());
        measureResp.setModelId(modelResp.getId());
        return measureResp;
    }

    public static DimensionReq convert(Dim dim, ModelDO modelDO) {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setName(dim.getName());
        dimensionReq.setBizName(dim.getBizName());
        dimensionReq.setDescription(dim.getName());
        dimensionReq.setSemanticType("CATEGORY");
        dimensionReq.setModelId(modelDO.getId());
        dimensionReq.setExpr(dim.getBizName());
        dimensionReq.setType("categorical");
        dimensionReq.setDescription(Objects.isNull(dim.getDescription()) ? "" : dim.getDescription());
        dimensionReq.setIsTag(dim.getIsTag());
        return dimensionReq;
    }

    public static MetricReq convert(Measure measure, ModelDO modelDO) {
        measure.setDatasourceId(modelDO.getId());
        MetricReq metricReq = new MetricReq();
        metricReq.setName(measure.getName());
        metricReq.setBizName(measure.getBizName().replaceFirst(modelDO.getBizName() + "_", ""));
        metricReq.setDescription(measure.getName());
        metricReq.setModelId(modelDO.getId());
        metricReq.setMetricType(MetricTypeEnum.ATOMIC);
        MetricTypeParams exprTypeParams = new MetricTypeParams();
        exprTypeParams.setExpr(measure.getBizName());
        exprTypeParams.setMeasures(Lists.newArrayList(measure));
        metricReq.setTypeParams(exprTypeParams);
        return metricReq;
    }

    public static DimensionReq convert(Identify identify, ModelDO modelDO) {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setName(identify.getName());
        dimensionReq.setBizName(identify.getBizName());
        dimensionReq.setDescription(identify.getName());
        dimensionReq.setSemanticType("CATEGORY");
        dimensionReq.setModelId(modelDO.getId());
        dimensionReq.setExpr(identify.getBizName());
        dimensionReq.setType(identify.getType());
        return dimensionReq;
    }

    public static List<ModelResp> convertList(List<ModelDO> modelDOS) {
        List<ModelResp> modelDescs = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(modelDOS)) {
            modelDescs = modelDOS.stream().map(ModelConverter::convert).collect(Collectors.toList());
        }
        return modelDescs;
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

    public static List<Dim> getDimToCreateDimension(ModelDetail modelDetail) {
        return modelDetail.getDimensions().stream()
                .filter(ModelConverter::isCreateDimension)
                .collect(Collectors.toList());
    }

    public static List<Measure> getMeasureToCreateMetric(ModelDetail modelDetail) {
        return modelDetail.getMeasures().stream()
                .filter(ModelConverter::isCreateMetric)
                .collect(Collectors.toList());
    }

    public static List<DimensionReq> convertDimensionList(ModelDO modelDO) {
        List<DimensionReq> dimensionReqs = Lists.newArrayList();
        ModelDetail modelDetail = JSONObject.parseObject(modelDO.getModelDetail(),
                ModelDetail.class);
        List<Dim> dims = getDimToCreateDimension(modelDetail);
        if (!CollectionUtils.isEmpty(dims)) {
            dimensionReqs = dims.stream().filter(dim -> StringUtils.isNotBlank(dim.getName()))
                    .map(dim -> convert(dim, modelDO)).collect(Collectors.toList());
        }
        List<Identify> identifies = modelDetail.getIdentifiers();
        if (CollectionUtils.isEmpty(identifies)) {
            return dimensionReqs;
        }
        dimensionReqs.addAll(identifies.stream()
                .filter(i -> i.getType().equalsIgnoreCase("primary"))
                .filter(i -> StringUtils.isNotBlank(i.getName()))
                .map(identify -> convert(identify, modelDO)).collect(Collectors.toList()));
        return dimensionReqs;
    }

    public static List<MetricReq> convertMetricList(ModelDO modelDO) {
        ModelDetail modelDetail = JSONObject.parseObject(modelDO.getModelDetail(),
                ModelDetail.class);
        List<Measure> measures = getMeasureToCreateMetric(modelDetail);
        if (CollectionUtils.isEmpty(measures)) {
            return Lists.newArrayList();
        }
        return measures.stream().map(measure -> convert(measure, modelDO)).collect(Collectors.toList());
    }

    private static ModelDetail getModelDetail(ModelReq modelReq) {
        ModelDetail modelDetail = new ModelDetail();
        BeanMapper.mapper(modelReq.getModelDetail(), modelDetail);
        List<Measure> measures = modelDetail.getMeasures();
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
            measure.setBizName(String.format("%s_%s", modelReq.getBizName(), measure.getBizName()));
        }
        return modelDetail;
    }
}
