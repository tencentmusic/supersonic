package com.tencent.supersonic.headless.model.domain.pojo;


import com.tencent.supersonic.headless.common.model.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.common.model.pojo.Entity;
import com.tencent.supersonic.headless.common.model.pojo.SchemaItem;
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
