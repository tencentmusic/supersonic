package com.tencent.supersonic.semantic.model.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.enums.DataTypeEnums;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.semantic.model.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.model.domain.pojo.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

public class DimensionConverter {

    public static DimensionDO convert(DimensionDO dimensionDO, DimensionReq dimensionReq) {
        BeanMapper.mapper(dimensionReq, dimensionDO);
        if (dimensionReq.getDefaultValues() != null) {
            dimensionDO.setDefaultValues(JSONObject.toJSONString(dimensionReq.getDefaultValues()));
        }
        if (!CollectionUtils.isEmpty(dimensionReq.getDimValueMaps())) {
            dimensionDO.setDimValueMaps(JSONObject.toJSONString(dimensionReq.getDimValueMaps()));
        } else {
            dimensionDO.setDimValueMaps(JSONObject.toJSONString(new ArrayList<>()));
        }
        if (Objects.nonNull(dimensionReq.getDataType())) {
            dimensionDO.setDataType(dimensionReq.getDataType().getType());
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
        dimensionDO.setStatus(StatusEnum.ONLINE.getCode());
        return dimensionDO;
    }

    public static DimensionResp convert2DimensionResp(DimensionDO dimensionDO,
                                                      Map<Long, String> fullPathMap,
                                                      Map<Long, DatasourceResp> datasourceRespMap) {
        DimensionResp dimensionResp = new DimensionResp();
        BeanUtils.copyProperties(dimensionDO, dimensionResp);
        dimensionResp.setFullPath(fullPathMap.get(dimensionDO.getModelId()) + "/" + dimensionDO.getBizName());
        dimensionResp.setDatasourceId(
                datasourceRespMap.getOrDefault(dimensionResp.getDatasourceId(), new DatasourceResp()).getId());
        dimensionResp.setDatasourceName(
                datasourceRespMap.getOrDefault(dimensionResp.getDatasourceId(), new DatasourceResp()).getName());
        dimensionResp.setDatasourceBizName(
                datasourceRespMap.getOrDefault(dimensionResp.getDatasourceId(), new DatasourceResp()).getBizName());
        if (dimensionDO.getDefaultValues() != null) {
            dimensionResp.setDefaultValues(JSONObject.parseObject(dimensionDO.getDefaultValues(), List.class));
        }
        if (Strings.isNotEmpty(dimensionDO.getDimValueMaps())) {
            dimensionResp.setDimValueMaps(JsonUtil.toList(dimensionDO.getDimValueMaps(), DimValueMap.class));
        }
        if (Strings.isNotEmpty(dimensionDO.getDataType())) {
            dimensionResp.setDataType(DataTypeEnums.of(dimensionDO.getDataType()));
        }
        return dimensionResp;
    }


    public static DimensionYamlTpl convert2DimensionYamlTpl(Dimension dimension) {
        DimensionYamlTpl dimensionYamlTpl = new DimensionYamlTpl();
        BeanUtils.copyProperties(dimension, dimensionYamlTpl);
        dimensionYamlTpl.setName(dimension.getBizName());
        dimensionYamlTpl.setOwners(dimension.getCreatedBy());
        return dimensionYamlTpl;
    }

    public static List<Dimension> dimensionInfo2Dimension(List<DimensionResp> dimensionResps) {
        List<Dimension> dimensions = new ArrayList<>();
        for (DimensionResp dimensionResp : dimensionResps) {
            Dimension dimension = new Dimension();
            BeanUtils.copyProperties(dimensionResp, dimension);
            dimensions.add(dimension);
        }
        return dimensions;
    }

}
