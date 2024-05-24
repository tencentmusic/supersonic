package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.TermReq;
import com.tencent.supersonic.headless.api.pojo.response.TermResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TermDO;
import com.tencent.supersonic.headless.server.persistence.mapper.TermMapper;
import com.tencent.supersonic.headless.server.service.TermService;
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
    public void saveOrUpdate(TermReq termSetReq, User user) {
        QueryWrapper<TermDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TermDO::getId, termSetReq.getId());
        TermDO termSetDO = getOne(queryWrapper);
        if (termSetDO == null) {
            termSetReq.createdBy(user.getName());
            termSetDO = new TermDO();
        }
        termSetReq.updatedBy(user.getName());
        BeanMapper.mapper(termSetReq, termSetDO);
        termSetDO.setAlias(JsonUtil.toString(termSetReq.getAlias()));
        saveOrUpdate(termSetDO);
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<TermResp> getTerms(Long domainId) {
        QueryWrapper<TermDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TermDO::getDomainId, domainId);
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
        return list.stream().map(this::convert).collect(
                Collectors.groupingBy(TermResp::getDomainId));
    }

    private TermResp convert(TermDO termSetDO) {
        TermResp termSetResp = new TermResp();
        BeanMapper.mapper(termSetDO, termSetResp);
        termSetResp.setAlias(JsonUtil.toList(termSetDO.getAlias(), String.class));
        return termSetResp;
    }

}
