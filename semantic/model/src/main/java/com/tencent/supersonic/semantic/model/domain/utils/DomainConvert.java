package com.tencent.supersonic.semantic.model.domain.utils;


import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.semantic.api.model.request.DomainReq;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.model.domain.dataobject.DomainDO;
import com.tencent.supersonic.semantic.model.domain.pojo.Domain;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

public class DomainConvert {

    public static Domain convert(DomainReq domainReq) {
        Domain domain = new Domain();
        BeanUtils.copyProperties(domainReq, domain);
        domain.setStatus(StatusEnum.ONLINE.getCode());
        return domain;
    }

    public static DomainDO convert(Domain domain, User user) {
        DomainDO domainDO = new DomainDO();
        BeanUtils.copyProperties(domain, domainDO);
        domainDO.setCreatedBy(user.getName());
        domainDO.setUpdatedBy(user.getName());
        domainDO.setCreatedAt(new Date());
        domainDO.setUpdatedAt(new Date());
        domainDO.setAdmin(String.join(",", domain.getAdmins()));
        domainDO.setAdminOrg(String.join(",", domain.getAdminOrgs()));
        domainDO.setViewer(String.join(",", domain.getViewers()));
        domainDO.setViewOrg(String.join(",", domain.getViewOrgs()));
        return domainDO;
    }

    public static DomainResp convert(DomainDO domainDO, Map<Long, String> domainFullPathMap) {
        DomainResp domainResp = new DomainResp();
        BeanUtils.copyProperties(domainDO, domainResp);
        domainResp.setFullPath(domainFullPathMap.get(domainDO.getId()));
        domainResp.setAdmins(StringUtils.isBlank(domainDO.getAdmin())
                ? Lists.newArrayList() : Arrays.asList(domainDO.getAdmin().split(",")));
        domainResp.setAdminOrgs(StringUtils.isBlank(domainDO.getAdminOrg())
                ? Lists.newArrayList() : Arrays.asList(domainDO.getAdminOrg().split(",")));
        domainResp.setViewers(StringUtils.isBlank(domainDO.getViewer())
                ? Lists.newArrayList() : Arrays.asList(domainDO.getViewer().split(",")));
        domainResp.setViewOrgs(StringUtils.isBlank(domainDO.getViewOrg())
                ? Lists.newArrayList() : Arrays.asList(domainDO.getViewOrg().split(",")));
        return domainResp;
    }

    public static DomainResp convert(DomainDO domainDO, Map<Long, String> domainFullPathMap,
            Map<Long, List<DimensionResp>> dimensionMap, Map<Long, List<MetricResp>> metricMap) {
        DomainResp domainResp = convert(domainDO, domainFullPathMap);
        domainResp.setDimensionCnt(dimensionMap.getOrDefault(domainResp.getId(), Lists.newArrayList()).size());
        domainResp.setMetricCnt(metricMap.getOrDefault(domainResp.getId(), Lists.newArrayList()).size());
        return domainResp;
    }


}
