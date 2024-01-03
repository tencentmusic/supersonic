package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.request.DomainReq;
import com.tencent.supersonic.headless.api.request.DomainUpdateReq;
import com.tencent.supersonic.headless.api.response.DomainResp;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DomainService {

    DomainResp getDomain(Long id);

    Map<Long, String> getDomainFullPath();

    void createDomain(DomainReq domainReq, User user);

    void updateDomain(DomainUpdateReq domainUpdateReq, User user);

    void deleteDomain(Long id);

    List<DomainResp> getDomainList();

    List<DomainResp> getDomainList(List<Long> domainIds);

    Map<Long, DomainResp> getDomainMap();

    List<DomainResp> getDomainListWithAdminAuth(User user);

    Set<DomainResp> getDomainAuthSet(User user, AuthType authTypeEnum);

    Set<DomainResp> getDomainChildren(List<Long> domainId);

}
