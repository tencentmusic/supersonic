package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.request.DomainUpdateReq;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DomainDO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DomainService {

    DomainResp getDomain(Long id);

    Map<Long, String> getDomainFullPath();

    DomainResp createDomain(DomainReq domainReq, User user);

    DomainResp updateDomain(DomainUpdateReq domainUpdateReq, User user);

    void deleteDomain(Long id);

    List<DomainResp> getDomainList();

    List<DomainResp> getDomainList(List<Long> domainIds);

    Map<Long, DomainResp> getDomainMap();

    List<DomainResp> getDomainListWithAdminAuth(User user);

    Set<DomainResp> getDomainAuthSet(User user, AuthType authTypeEnum);

    List<DomainDO> getDomainByBizName(String bizName);

    Set<DomainResp> getDomainChildren(List<Long> domainId);
}
