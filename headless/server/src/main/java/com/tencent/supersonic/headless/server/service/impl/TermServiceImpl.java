package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.Term;
import com.tencent.supersonic.headless.api.pojo.request.TermSetReq;
import com.tencent.supersonic.headless.api.pojo.response.TermSetResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TermSetDO;
import com.tencent.supersonic.headless.server.persistence.mapper.TermSetMapper;
import com.tencent.supersonic.headless.server.service.TermService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class TermServiceImpl extends ServiceImpl<TermSetMapper, TermSetDO> implements TermService {

    @Override
    public void saveOrUpdate(TermSetReq termSetReq, User user) {
        QueryWrapper<TermSetDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TermSetDO::getDomainId, termSetReq.getDomainId());
        TermSetDO termSetDO = getOne(queryWrapper);
        if (termSetDO == null) {
            termSetReq.createdBy(user.getName());
            termSetDO = new TermSetDO();
        }
        termSetReq.updatedBy(user.getName());
        BeanMapper.mapper(termSetReq, termSetDO);
        termSetDO.setTerms(JsonUtil.toString(termSetReq.getTerms()));
        saveOrUpdate(termSetDO);
    }

    @Override
    public TermSetResp getTermSet(Long domainId) {
        QueryWrapper<TermSetDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TermSetDO::getDomainId, domainId);
        TermSetDO termSetDO = getOne(queryWrapper);
        TermSetResp termSetResp = new TermSetResp(domainId);
        if (termSetDO == null) {
            return termSetResp;
        }
        return convert(termSetDO);
    }

    @Override
    public Map<Long, List<Term>> getTermSets(Set<Long> domainIds) {
        QueryWrapper<TermSetDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TermSetDO::getDomainId, domainIds);
        List<TermSetDO> list = list(queryWrapper);
        return list.stream().map(this::convert).collect(
                Collectors.toMap(TermSetResp::getDomainId, TermSetResp::getTerms, (k1, k2) -> k1));
    }

    private TermSetResp convert(TermSetDO termSetDO) {
        TermSetResp termSetResp = new TermSetResp();
        BeanMapper.mapper(termSetDO, termSetResp);
        termSetResp.setTerms(JsonUtil.toList(termSetDO.getTerms(), Term.class));
        return termSetResp;
    }

}
