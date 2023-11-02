package com.tencent.supersonic.semantic.api.model.request;


import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.pojo.Entity;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class ModelReq extends SchemaItem {

    private Long domainId = 0L;

    private Integer isOpen = 0;

    private String alias;

    private List<String> viewers = new ArrayList<>();

    private List<String> viewOrgs = new ArrayList<>();

    private List<String> admins = new ArrayList<>();

    private List<String> adminOrgs = new ArrayList<>();

    private Entity entity;

    private List<DrillDownDimension> drillDownDimensions;

    public String getViewer() {
        return String.join(",", viewers);
    }

    public String getViewOrg() {
        return String.join(",", viewOrgs);
    }


    public String getAdmin() {
        return String.join(",", admins);
    }

    public String getAdminOrg() {
        return String.join(",", adminOrgs);
    }

}
