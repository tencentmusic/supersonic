package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.ViewDetail;
import lombok.Data;

import java.util.List;

@Data
public class ViewReq extends SchemaItem {

    private Long domainId;

    private ViewDetail viewDetail;

    private String alias;

    private String filterSql;

    private QueryConfig queryConfig;

    private List<String> admins;

    private List<String> adminOrgs;

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
