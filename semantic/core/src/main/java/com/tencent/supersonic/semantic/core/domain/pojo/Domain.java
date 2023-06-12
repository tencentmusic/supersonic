package com.tencent.supersonic.semantic.core.domain.pojo;


import com.tencent.supersonic.semantic.api.core.request.DomainReq;
import com.tencent.supersonic.common.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.SchemaItem;
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

    public static Domain create(DomainReq domainCmd) {
        Domain domain = new Domain();
        BeanUtils.copyProperties(domainCmd, domain);
        domain.setStatus(StatusEnum.ONLINE.getCode());
        return domain;
    }


}
