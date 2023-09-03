package com.tencent.supersonic.semantic.model.domain.pojo;


import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.semantic.api.model.pojo.Entity;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;


@Data
public class Model extends SchemaItem {

    private Long domainId;

    private Integer isOpen;

    private List<String> viewers;

    private List<String> viewOrgs;

    private List<String> admins;

    private List<String> adminOrgs;

    private Entity entity;

    public static Model create(ModelReq modelReq) {
        Model model = new Model();
        BeanUtils.copyProperties(modelReq, model);
        model.setStatus(StatusEnum.ONLINE.getCode());
        return model;
    }


}
