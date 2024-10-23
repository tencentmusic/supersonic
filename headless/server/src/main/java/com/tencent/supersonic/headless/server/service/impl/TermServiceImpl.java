package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.TermReq;
import com.tencent.supersonic.headless.api.pojo.response.TermResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TermDO;
import com.tencent.supersonic.headless.server.persistence.mapper.TermMapper;
import com.tencent.supersonic.headless.server.service.TermService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TermServiceImpl extends ServiceImpl<TermMapper, TermDO> implements TermService {

    @Override
    public void saveOrUpdate(TermReq termReq, User user) {
        QueryWrapper<TermDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TermDO::getId, termReq.getId());
        TermDO termSetDO = getOne(queryWrapper);
        if (termSetDO == null) {
            termReq.createdBy(user.getName());
            termSetDO = new TermDO();
        }
        termReq.updatedBy(user.getName());
        convert(termReq, termSetDO);
        saveOrUpdate(termSetDO);
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public void deleteBatch(MetaBatchReq metaBatchReq) {
        if (CollectionUtils.isEmpty(metaBatchReq.getIds())) {
            throw new RuntimeException("术语ID不可为空");
        }
        removeBatchByIds(metaBatchReq.getIds());
    }

    @Override
    public List<TermResp> getTerms(Long domainId, String queryKey) {
        QueryWrapper<TermDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TermDO::getDomainId, domainId);
        if (StringUtils.isNotBlank(queryKey)) {
            queryWrapper.lambda().and(i -> i.like(TermDO::getName, queryKey).or()
                    .like(TermDO::getDescription, queryKey).or().like(TermDO::getAlias, queryKey));
        }
        List<TermDO> termDOS = list(queryWrapper);
        return termDOS.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<TermResp>> getTermSets(Set<Long> domainIds) {
        if (CollectionUtils.isEmpty(domainIds)) {
            return new HashMap<>();
        }
        QueryWrapper<TermDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TermDO::getDomainId, domainIds);
        List<TermDO> list = list(queryWrapper);
        return list.stream().map(this::convert)
                .collect(Collectors.groupingBy(TermResp::getDomainId));
    }

    private TermResp convert(TermDO termDO) {
        TermResp termSetResp = new TermResp();
        BeanMapper.mapper(termDO, termSetResp);
        termSetResp.setAlias(JsonUtil.toList(termDO.getAlias(), String.class));
        termSetResp.setRelatedMetrics(JsonUtil.toList(termDO.getRelatedMetrics(), Long.class));
        termSetResp.setRelateDimensions(JsonUtil.toList(termDO.getRelatedDimensions(), Long.class));
        return termSetResp;
    }

    private void convert(TermReq termReq, TermDO termDO) {
        BeanMapper.mapper(termReq, termDO);
        termDO.setAlias(JsonUtil.toString(termReq.getAlias()));
        termDO.setRelatedDimensions(JsonUtil.toString(termReq.getRelateDimensions()));
        termDO.setRelatedMetrics(JsonUtil.toString(termReq.getRelatedMetrics()));
    }
}
