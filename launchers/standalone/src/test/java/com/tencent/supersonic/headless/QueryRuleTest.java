package com.tencent.supersonic.headless;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.ActionInfo;
import com.tencent.supersonic.headless.api.pojo.RuleInfo;
import com.tencent.supersonic.headless.api.pojo.enums.QueryRuleType;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryRuleResp;
import com.tencent.supersonic.headless.server.web.service.QueryRuleService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryRuleTest extends BaseTest {

    @Autowired
    private QueryRuleService queryRuleService;

    private User user = User.getFakeUser();

    public QueryRuleReq addSystemRule() {
        QueryRuleReq queryRuleReq = new QueryRuleReq();
        queryRuleReq.setPriority(0);
        queryRuleReq.setRuleType(QueryRuleType.ADD_DATE);
        queryRuleReq.setName("全局默认时间设置");
        queryRuleReq.setBizName("global date config");
        RuleInfo rule = new RuleInfo();
        rule.setMode(RuleInfo.Mode.BEFORE);
        List parameters = new ArrayList(Arrays.asList(3));
        rule.setParameters(parameters);
        queryRuleReq.setRule(rule);
        return queryRuleReq;
    }

    public QueryRuleReq addUserRule1() {
        QueryRuleReq queryRuleReq = new QueryRuleReq();
        queryRuleReq.setDataSetId(2L);
        queryRuleReq.setPriority(1);
        queryRuleReq.setRuleType(QueryRuleType.ADD_DATE);
        queryRuleReq.setName("规则_1");
        queryRuleReq.setBizName("rule_1");

        RuleInfo rule = new RuleInfo();
        rule.setMode(RuleInfo.Mode.BEFORE);
        List parameters = new ArrayList(Arrays.asList(4));
        rule.setParameters(parameters);
        queryRuleReq.setRule(rule);
        return queryRuleReq;
    }

    public QueryRuleReq addUserRule2() {
        QueryRuleReq queryRuleReq = new QueryRuleReq();
        queryRuleReq.setDataSetId(2L);
        queryRuleReq.setPriority(1);
        queryRuleReq.setRuleType(QueryRuleType.ADD_SELECT);
        queryRuleReq.setName("规则_2");
        queryRuleReq.setBizName("rule_2");

        RuleInfo rule = new RuleInfo();
        rule.setMode(RuleInfo.Mode.EXIST);
        rule.setParameters(Arrays.asList("singer_id"));
        queryRuleReq.setRule(rule);

        ActionInfo action = new ActionInfo();
        List parameters = new ArrayList(Arrays.asList("c1", "c2"));
        action.setOut(parameters);
        queryRuleReq.setAction(action);
        return queryRuleReq;
    }

    @Test
    public void testAddQueryRule() {
        QueryRuleReq queryRuleReqSys = addSystemRule();
        QueryRuleResp queryRuleResp = queryRuleService.addQueryRule(queryRuleReqSys, user);
        QueryRuleResp queryRule = queryRuleService.getQueryRuleById(1L, user);
        Assert.assertEquals(queryRuleResp.getPriority().intValue(), 0);
        Assert.assertEquals(queryRule.getPriority().intValue(), 0);

        QueryRuleReq queryRuleReq1 = addUserRule1();
        QueryRuleResp queryRuleResp1 = queryRuleService.addQueryRule(queryRuleReq1, user);
        queryRuleResp1.setName("规则_1_1");
        QueryRuleReq queryRuleReq11 = new QueryRuleReq();
        BeanUtils.copyProperties(queryRuleResp1, queryRuleReq11);
        queryRuleService.updateQueryRule(queryRuleReq11, user);

        QueryRuleReq queryRuleReq2 = addUserRule2();
        queryRuleService.addQueryRule(queryRuleReq2, user);

        QueryRuleFilter queryRuleFilter = new QueryRuleFilter();
        List<QueryRuleResp> queryRuleList = queryRuleService.getQueryRuleList(queryRuleFilter, user);
        queryRuleList.size();
    }
}