package com.tencent.supersonic.semantic.model.domain.utils;


import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.pojo.Entity;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import com.tencent.supersonic.semantic.model.domain.pojo.Model;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class ModelConvert {

    public static Model convert(ModelReq modelReq) {
        Model model = new Model();
        BeanUtils.copyProperties(modelReq, model);
        model.setStatus(StatusEnum.ONLINE.getCode());
        return model;
    }

    public static ModelDO convert(Model model, User user) {
        ModelDO modelDO = new ModelDO();
        BeanUtils.copyProperties(model, modelDO);
        modelDO.setCreatedBy(user.getName());
        modelDO.setUpdatedBy(user.getName());
        modelDO.setCreatedAt(new Date());
        modelDO.setUpdatedAt(new Date());
        modelDO.setAdmin(String.join(",", model.getAdmins()));
        modelDO.setAdminOrg(String.join(",", model.getAdminOrgs()));
        modelDO.setViewer(String.join(",", model.getViewers()));
        modelDO.setViewOrg(String.join(",", model.getViewOrgs()));
        modelDO.setEntity(JsonUtil.toString(model.getEntity()));
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
