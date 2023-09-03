package com.tencent.supersonic.semantic.api.model.response;

import com.tencent.supersonic.semantic.api.model.pojo.Entity;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class ModelResp extends SchemaItem {

    private Long domainId;

    private List<String> viewers;

    private List<String> viewOrgs;

    private List<String> admins;

    private List<String> adminOrgs;

    private Integer isOpen = 0;

    private Integer dimensionCnt;

    private Integer metricCnt;

    private Entity entity;

    private String fullPath;

    public boolean openToAll() {
        return isOpen != null && isOpen == 1;
    }

}
