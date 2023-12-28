package com.tencent.supersonic.headless.model.domain.pojo;


import com.tencent.supersonic.headless.common.model.pojo.Entity;
import com.tencent.supersonic.headless.common.model.request.DomainReq;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.headless.common.model.pojo.SchemaItem;
import java.util.List;
import lombok.Data;
import org.springframework.beans.BeanUtils;


@Data
public class Domain extends SchemaItem {

    private Long parentId;

    private Integer isOpen;

    private List<String> viewers;

    private List<String> viewOrgs;

    private List<String> admins;

    private List<String> adminOrgs;

    private Entity entity;

    public static Domain create(DomainReq domainCmd) {
        Domain domain = new Domain();
        BeanUtils.copyProperties(domainCmd, domain);
        domain.setStatus(StatusEnum.ONLINE.getCode());
        return domain;
    }

}
