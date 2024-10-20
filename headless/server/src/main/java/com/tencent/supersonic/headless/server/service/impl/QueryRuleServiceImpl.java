package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryRuleResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.QueryRuleDO;
import com.tencent.supersonic.headless.server.persistence.repository.QueryRuleRepository;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.QueryRuleService;
import com.tencent.supersonic.headless.server.utils.QueryRuleConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class QueryRuleServiceImpl implements QueryRuleService {

    private final QueryRuleRepository queryRuleRepository;
    private final DataSetService dataSetService;

    public QueryRuleServiceImpl(QueryRuleRepository queryRuleRepository,
            DataSetService dataSetService) {
        this.queryRuleRepository = queryRuleRepository;
        this.dataSetService = dataSetService;
    }

    @Override
    public QueryRuleResp addQueryRule(QueryRuleReq queryRuleReq, User user) {
        checkPermission(queryRuleReq, user);
        QueryRuleDO queryRuleDO = QueryRuleConverter.convert2DO(queryRuleReq);

        Date date = new Date();
        queryRuleDO.setCreatedBy(user.getName());
        queryRuleDO.setCreatedAt(date);
        queryRuleDO.setUpdatedBy(user.getName());
        queryRuleDO.setUpdatedAt(date);
        queryRuleDO.setStatus(StatusEnum.ONLINE.getCode());
        queryRuleDO.setId(null);

        queryRuleRepository.create(queryRuleDO);
        return getQueryRuleById(queryRuleDO.getId(), user);
    }

    @Override
    public QueryRuleResp updateQueryRule(QueryRuleReq queryRuleReq, User user) {
        checkPermission(queryRuleReq, user);
        QueryRuleDO queryRuleDO = queryRuleRepository.getQueryRuleById(queryRuleReq.getId());
        QueryRuleDO queryRuleNew = QueryRuleConverter.convert2DO(queryRuleReq);
        BeanMapper.mapper(queryRuleNew, queryRuleDO);
        queryRuleDO.setUpdatedAt(new Date());
        queryRuleDO.setUpdatedBy(user.getName());
        queryRuleRepository.update(queryRuleDO);
        return getQueryRuleById(queryRuleDO.getId(), user);
    }

    @Override
    public Boolean dropQueryRule(Long id, User user) {
        QueryRuleDO queryRuleDO = queryRuleRepository.getQueryRuleById(id);
        checkPermission(queryRuleDO, user);
        queryRuleDO.setStatus(StatusEnum.DELETED.getCode());
        queryRuleRepository.update(queryRuleDO);
        return true;
    }

    @Override
    public QueryRuleResp getQueryRuleById(Long id, User user) {
        QueryRuleDO queryRuleDO = queryRuleRepository.getQueryRuleById(id);
        QueryRuleResp queryRuleResp = QueryRuleConverter.convert2Resp(queryRuleDO);
        return queryRuleResp;
    }

    @Override
    public List<QueryRuleResp> getQueryRuleList(QueryRuleFilter queryRuleFilter, User user) {
        List<QueryRuleDO> queryRules = queryRuleRepository.getQueryRules(queryRuleFilter);
        List<QueryRuleResp> queryRuleRespList = QueryRuleConverter.convert2RespList(queryRules);
        return queryRuleRespList;
    }

    private void checkPermission(QueryRuleReq queryRuleReq, User user) {
        String userName = user.getName();
        if (Objects.nonNull(queryRuleReq.getDataSetId())) {
            DataSetResp dataSet = dataSetService.getDataSet(queryRuleReq.getDataSetId());
            if (dataSet.getAdmins().contains(userName)
                    || dataSet.getCreatedBy().equalsIgnoreCase(userName)) {
                log.debug(String.format("user:%s, queryRuleReq:%s", userName, queryRuleReq));
                return;
            }
            throw new RuntimeException("用户暂无权限变更数据集的规则, 请确认");
        }
    }

    private void checkPermission(QueryRuleDO queryRuleDO, User user) {
        String userName = user.getName();
        if (Objects.nonNull(queryRuleDO.getDataSetId())) {
            DataSetResp dataSet = dataSetService.getDataSet(queryRuleDO.getDataSetId());
            if (dataSet.getAdmins().contains(userName)
                    || dataSet.getCreatedBy().equalsIgnoreCase(userName)) {
                log.debug(String.format("user:%s, queryRuleDO:%s", userName, queryRuleDO));
                return;
            }
            throw new RuntimeException("用户暂无权限变更数据集的规则, 请确认");
        }
    }
}
