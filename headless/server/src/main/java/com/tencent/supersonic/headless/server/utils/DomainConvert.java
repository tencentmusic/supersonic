package com.tencent.supersonic.headless.server.utils;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DomainDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DomainConvert {

    public static DomainDO convert(DomainReq domainReq, User user) {
        DomainDO domainDO = new DomainDO();
        domainReq.createdBy(user.getName());
        BeanUtils.copyProperties(domainReq, domainDO);
        domainDO.setAdmin(String.join(",", domainReq.getAdmins()));
        domainDO.setAdminOrg(String.join(",", domainReq.getAdminOrgs()));
        domainDO.setViewer(String.join(",", domainReq.getViewers()));
        domainDO.setViewOrg(String.join(",", domainReq.getViewOrgs()));
        return domainDO;
    }

    public static DomainResp convert(DomainDO domainDO, Map<Long, String> domainFullPathMap) {
        if (Objects.isNull(domainDO)) {
            return null;
        }
        DomainResp domainResp = new DomainResp();
        BeanUtils.copyProperties(domainDO, domainResp);
        domainResp.setFullPath(domainFullPathMap.get(domainDO.getId()));
        domainResp.setAdmins(StringUtils.isBlank(domainDO.getAdmin()) ? Lists.newArrayList()
                : Arrays.asList(domainDO.getAdmin().split(",")));
        domainResp.setAdminOrgs(StringUtils.isBlank(domainDO.getAdminOrg()) ? Lists.newArrayList()
                : Arrays.asList(domainDO.getAdminOrg().split(",")));
        domainResp.setViewers(StringUtils.isBlank(domainDO.getViewer()) ? Lists.newArrayList()
                : Arrays.asList(domainDO.getViewer().split(",")));
        domainResp.setViewOrgs(StringUtils.isBlank(domainDO.getViewOrg()) ? Lists.newArrayList()
                : Arrays.asList(domainDO.getViewOrg().split(",")));
        return domainResp;
    }

    public static DomainResp convert(DomainDO domainDO) {
        return convert(domainDO, new HashMap<>());
    }
}
