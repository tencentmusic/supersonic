package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.api.pojo.request.QueryRuleFilter;
import com.tencent.supersonic.headless.server.persistence.dataobject.QueryRuleDO;

import java.util.List;

public interface QueryRuleRepository {

    Integer create(QueryRuleDO queryRuleDO);

    Integer update(QueryRuleDO queryRuleDO);

    QueryRuleDO getQueryRuleById(Long id);

    List<QueryRuleDO> getQueryRules(QueryRuleFilter filter);

}
