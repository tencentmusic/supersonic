package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModelReq extends SchemaItem {

    private Long databaseId;

    private Long domainId;

    private String filterSql;

    private Integer isOpen;

    private List<DrillDownDimension> drillDownDimensions;

    private String alias;

    private String sourceType;

    private ModelDetail modelDetail;

    private List<String> viewers;

    private List<String> viewOrgs;

    private List<String> admins;

    private List<String> adminOrgs;

    private Map<String, Object> ext;

    public String getViewer() {
        if (viewers == null) {
            return null;
        }
        return String.join(",", viewers);
    }

    public String getViewOrg() {
        if (viewOrgs == null) {
            return null;
        }
        return String.join(",", viewOrgs);
    }

    public String getAdmin() {
        if (admins == null) {
            return null;
        }
        return String.join(",", admins);
    }

    public String getAdminOrg() {
        if (adminOrgs == null) {
            return null;
        }
        return String.join(",", adminOrgs);
    }
}
