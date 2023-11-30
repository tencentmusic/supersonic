package com.tencent.supersonic.semantic.api.model.request;


import com.google.common.collect.Lists;
import com.tencent.supersonic.semantic.api.model.enums.DimensionTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.pojo.ModelDetail;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Data
public class ModelReq extends SchemaItem {

    private Long databaseId;

    private Long domainId;

    private String filterSql;

    private Integer isOpen;

    private List<DrillDownDimension> drillDownDimensions;

    private String alias;

    private ModelDetail modelDetail;

    private List<String> viewers = new ArrayList<>();

    private List<String> viewOrgs = new ArrayList<>();

    private List<String> admins = new ArrayList<>();

    private List<String> adminOrgs = new ArrayList<>();

    public List<Dim> getTimeDimension() {
        if (CollectionUtils.isEmpty(modelDetail.getDimensions())) {
            return Lists.newArrayList();
        }
        return modelDetail.getDimensions().stream()
                .filter(dim -> DimensionTypeEnum.time.name().equalsIgnoreCase(dim.getType()))
                .collect(Collectors.toList());
    }

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
