package com.tencent.supersonic.semantic.model.domain.utils;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.pojo.Entity;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import java.util.Arrays;
import java.util.Map;

public class ModelConvert {

    public static ModelDO convert(ModelReq modelReq) {
        ModelDO modelDO = new ModelDO();
        BeanMapper.mapper(modelReq, modelDO);
        modelDO.setEntity(JsonUtil.toString(modelReq.getEntity()));
        modelDO.setDrillDownDimensions(JsonUtil.toString(modelReq.getDrillDownDimensions()));
        modelDO.setStatus(StatusEnum.ONLINE.getCode());
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
        modelResp.setEntity(JsonUtil.toObject(modelDO.getEntity(), Entity.class));
        modelResp.setDrillDownDimensions(JsonUtil.toList(modelDO.getDrillDownDimensions(), DrillDownDimension.class));
        return modelResp;
    }

    public static ModelResp convert(ModelDO modelDO,
                                    Map<Long, DomainResp> domainRespMap) {
        ModelResp modelResp = convert(modelDO);
        DomainResp domainResp = domainRespMap.get(modelResp.getDomainId());
        if (domainResp != null) {
            String fullBizNamePath = domainResp.getFullPath() + modelResp.getBizName();
            modelResp.setFullPath(fullBizNamePath);
        }
        return modelResp;
    }


}
