package com.tencent.supersonic.headless.server.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.Term;
import com.tencent.supersonic.headless.api.pojo.request.TermSetReq;
import com.tencent.supersonic.headless.api.pojo.response.TermSetResp;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TermService {

    void saveOrUpdate(TermSetReq termSetReq, User user);

    TermSetResp getTermSet(Long domainId);

    Map<Long, List<Term>> getTermSets(Set<Long> domainIds);

}
