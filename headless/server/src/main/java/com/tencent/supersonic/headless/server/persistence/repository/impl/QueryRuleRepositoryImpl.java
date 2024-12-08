package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleFilter;
import com.tencent.supersonic.headless.server.persistence.dataobject.QueryRuleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.QueryRuleMapper;
import com.tencent.supersonic.headless.server.persistence.repository.QueryRuleRepository;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QueryRuleRepositoryImpl implements QueryRuleRepository {

    private final QueryRuleMapper mapper;

    public QueryRuleRepositoryImpl(QueryRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Integer create(QueryRuleDO queryRuleDO) {
        return mapper.insert(queryRuleDO);
    }

    @Override
    public Integer update(QueryRuleDO queryRuleDO) {
        return mapper.updateById(queryRuleDO);
    }

    @Override
    public QueryRuleDO getQueryRuleById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<QueryRuleDO> getQueryRules(QueryRuleFilter filter) {
        QueryWrapper<QueryRuleDO> wrapper = new QueryWrapper<>();
        if (CollectionUtils.isNotEmpty(filter.getRuleIds())) {
            wrapper.lambda().in(QueryRuleDO::getId, filter.getRuleIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getDataSetIds())) {
            wrapper.lambda().in(QueryRuleDO::getDataSetId, filter.getDataSetIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getStatusList())) {
            wrapper.lambda().in(QueryRuleDO::getStatus, filter.getStatusList());
        }
        wrapper.lambda().gt(QueryRuleDO::getPriority, 0);
        List<QueryRuleDO> queryRuleDOList = mapper.selectList(wrapper);

        QueryWrapper<QueryRuleDO> wrapperSys = new QueryWrapper<>();
        // 返回系统设置的规则
        wrapperSys.lambda().or().eq(QueryRuleDO::getPriority, 0L);
        List<QueryRuleDO> queryRuleDOListSys = mapper.selectList(wrapperSys);

        queryRuleDOList.addAll(queryRuleDOListSys);
        return queryRuleDOList;
    }
}
