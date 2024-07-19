package com.tencent.supersonic.headless.server.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.TermReq;
import com.tencent.supersonic.headless.api.pojo.response.TermResp;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TermService {

    void saveOrUpdate(TermReq termSetReq, User user);

    void delete(Long id);

    List<TermResp> getTerms(Long domainId);

    Map<Long, List<TermResp>> getTermSets(Set<Long> domainIds);

}
