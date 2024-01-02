package com.tencent.supersonic.headless.common.server.response;

import com.tencent.supersonic.headless.common.server.pojo.Entity;
import com.tencent.supersonic.headless.common.server.pojo.SchemaItem;
import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

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

    private Integer dimensionCnt;

    private Integer metricCnt;

    private Entity entity;

    private boolean hasEditPermission = false;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DomainResp that = (DomainResp) o;
        if (getId() == null || that.getId() == null) {
            return false;
        }
        return Objects.equals(getId().intValue(), that.getId().intValue());
    }

    @Override
    public int hashCode() {
        if (getId() == null) {
            return 0;
        }
        return getId().hashCode();
    }
}
