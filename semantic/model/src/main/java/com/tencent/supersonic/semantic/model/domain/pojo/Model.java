package com.tencent.supersonic.semantic.model.domain.pojo;


import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.pojo.Entity;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;
import java.util.List;


@Data
public class Model extends SchemaItem {

    private Long domainId;

    private Integer isOpen;

    private String alias;

    private List<String> viewers;

    private List<String> viewOrgs;

    private List<String> admins;

    private List<String> adminOrgs;

    private Entity entity;

    private List<DrillDownDimension> drillDownDimensions;

}
