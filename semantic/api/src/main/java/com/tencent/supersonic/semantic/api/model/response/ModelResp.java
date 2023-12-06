package com.tencent.supersonic.semantic.api.model.response;

import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.ModelDetail;
import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class ModelResp extends SchemaItem {

    private Long domainId;

    private Long databaseId;

    private ModelDetail modelDetail;

    private String depends;

    private String filterSql;

    private List<String> viewers = new ArrayList<>();

    private List<String> viewOrgs = new ArrayList<>();

    private List<String> admins = new ArrayList<>();

    private List<String> adminOrgs = new ArrayList<>();

    private Integer isOpen;

    private List<DrillDownDimension> drillDownDimensions;

    private String alias;

    private String fullPath;

    private Integer dimensionCnt;

    private Integer metricCnt;

    public boolean openToAll() {
        return isOpen != null && isOpen == 1;
    }

    public Identify getPrimaryIdentify() {
        if (modelDetail == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(modelDetail.getIdentifiers())) {
            return null;
        }
        for (Identify identify : modelDetail.getIdentifiers()) {
            if (!CollectionUtils.isEmpty(identify.getEntityNames())) {
                return identify;
            }
        }
        return null;
    }

}
