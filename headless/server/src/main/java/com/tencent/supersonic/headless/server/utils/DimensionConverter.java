package com.tencent.supersonic.headless.server.utils;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.enums.DataTypeEnums;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import com.tencent.supersonic.headless.api.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.request.DimensionReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DimensionDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DimensionConverter {

    public static DimensionDO convert(DimensionDO dimensionDO, DimensionReq dimensionReq) {
        BeanMapper.mapper(dimensionReq, dimensionDO);
        if (dimensionReq.getDefaultValues() != null) {
            dimensionDO.setDefaultValues(JSONObject.toJSONString(dimensionReq.getDefaultValues()));
        }
        if (!CollectionUtils.isEmpty(dimensionReq.getDimValueMaps())) {
            List<DimValueMap> dimValueMaps = dimensionReq.getDimValueMaps();
            dimValueMaps.stream().forEach(dimValueMap -> {
                dimValueMap.setTechName(dimValueMap.getValue());
            });

            dimensionDO.setDimValueMaps(JSONObject.toJSONString(dimensionReq.getDimValueMaps()));
        } else {
            dimensionDO.setDimValueMaps(JSONObject.toJSONString(new ArrayList<>()));
        }
        if (Objects.nonNull(dimensionReq.getDataType())) {
            dimensionDO.setDataType(dimensionReq.getDataType().getType());
        }
        if (dimensionReq.getExt() != null) {
            dimensionDO.setExt(JSONObject.toJSONString(dimensionReq.getExt()));
        }
        if (Objects.nonNull(dimensionReq.getTypeParams())) {
            dimensionDO.setTypeParams(JSONObject.toJSONString(dimensionReq.getTypeParams()));
        }
        return dimensionDO;
    }

    public static DimensionDO convert2DimensionDO(DimensionReq dimensionReq) {
        DimensionDO dimensionDO = new DimensionDO();
        BeanMapper.mapper(dimensionReq, dimensionDO);
        if (dimensionReq.getDefaultValues() != null) {
            dimensionDO.setDefaultValues(JSONObject.toJSONString(dimensionReq.getDefaultValues()));
        }
        if (dimensionReq.getDimValueMaps() != null) {
            dimensionDO.setDimValueMaps(JSONObject.toJSONString(dimensionReq.getDimValueMaps()));
        }
        if (Objects.nonNull(dimensionReq.getDataType())) {
            dimensionDO.setDataType(dimensionReq.getDataType().getType());
        }
        if (dimensionReq.getExt() != null) {
            dimensionDO.setExt(JSONObject.toJSONString(dimensionReq.getExt()));
        }
        dimensionDO.setStatus(StatusEnum.ONLINE.getCode());
        return dimensionDO;
    }

    public static DimensionResp convert2DimensionResp(DimensionDO dimensionDO,
            Map<Long, ModelResp> modelRespMap) {
        DimensionResp dimensionResp = new DimensionResp();
        BeanUtils.copyProperties(dimensionDO, dimensionResp);
        dimensionResp.setModelName(
                modelRespMap.getOrDefault(dimensionResp.getModelId(), new ModelResp()).getName());
        dimensionResp.setModelBizName(modelRespMap
                .getOrDefault(dimensionResp.getModelId(), new ModelResp()).getBizName());
        if (dimensionDO.getDefaultValues() != null) {
            dimensionResp.setDefaultValues(
                    JSONObject.parseObject(dimensionDO.getDefaultValues(), List.class));
        }
        dimensionResp.setModelFilterSql(modelRespMap
                .getOrDefault(dimensionResp.getModelId(), new ModelResp()).getFilterSql());
        if (StringUtils.isNotEmpty(dimensionDO.getDimValueMaps())) {
            dimensionResp.setDimValueMaps(
                    JsonUtil.toList(dimensionDO.getDimValueMaps(), DimValueMap.class));
        }
        if (StringUtils.isNotEmpty(dimensionDO.getDataType())) {
            dimensionResp.setDataType(DataTypeEnums.of(dimensionDO.getDataType()));
        }
        if (dimensionDO.getExt() != null) {
            dimensionResp.setExt(JSONObject.parseObject(dimensionDO.getExt(), Map.class));
        }
        if (StringUtils.isNoneBlank(dimensionDO.getTypeParams())) {
            dimensionResp.setTypeParams(JSONObject.parseObject(dimensionDO.getTypeParams(),
                    DimensionTimeTypeParams.class));
        }
        dimensionResp.setType(getType(dimensionDO.getType()));
        dimensionResp.setTypeEnum(TypeEnums.DIMENSION);
        dimensionResp.setIsTag(dimensionDO.getIsTag());
        dimensionResp.setDomainId(modelRespMap
                .getOrDefault(dimensionResp.getModelId(), new ModelResp()).getDomainId());
        return dimensionResp;
    }

    public static DimensionResp convert2DimensionResp(DimensionDO dimensionDO) {
        return convert2DimensionResp(dimensionDO, new HashMap<>());
    }

    private static DimensionType getType(String type) {
        try {
            // Support compatibility with legacy data.
            IdentifyType.valueOf(type.toLowerCase());
            return DimensionType.primary_key;
        } catch (IllegalArgumentException e) {
            return DimensionType.valueOf(type);
        }
    }

    public static List<DimensionResp> filterByDataSet(List<DimensionResp> dimensionResps,
            DataSetResp dataSetResp) {
        return dimensionResps.stream()
                .filter(dimensionResp -> dataSetResp.dimensionIds().contains(dimensionResp.getId())
                        || dataSetResp.getAllIncludeAllModels()
                                .contains(dimensionResp.getModelId()))
                .collect(Collectors.toList());
    }
}
