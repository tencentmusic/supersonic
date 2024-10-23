package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.TermReq;
import com.tencent.supersonic.headless.api.pojo.response.TermResp;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TermService {

    void saveOrUpdate(TermReq termSetReq, User user);

    void delete(Long id);

    void deleteBatch(MetaBatchReq metaBatchReq);

    List<TermResp> getTerms(Long domainId, String queryKey);

    Map<Long, List<TermResp>> getTermSets(Set<Long> domainIds);
}
