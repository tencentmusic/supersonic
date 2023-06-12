package com.tencent.supersonic.semantic.api.core.response;

import com.tencent.supersonic.common.pojo.SchemaItem;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DomainResp extends SchemaItem {

    private Long parentId;

    private String fullPath;

    private List<String> viewers;

    private List<String> viewOrgs;

    private List<String> admins;

    private List<String> adminOrgs;

    private Integer isOpen = 0;


}
