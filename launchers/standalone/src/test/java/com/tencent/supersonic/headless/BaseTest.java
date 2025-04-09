package com.tencent.supersonic.headless;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.persistence.dataobject.DomainDO;
import com.tencent.supersonic.headless.server.persistence.repository.DomainRepository;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.util.DataUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.now;

public class BaseTest extends BaseApplication {

    @Autowired
    protected SemanticLayerService semanticLayerService;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    protected SchemaService schemaService;
    @Autowired
    private AgentService agentService;
    @Autowired
    protected DatabaseService databaseService;
    @Autowired
    protected DataSetService dataSetService;

    protected Agent agent;
    protected SemanticSchema schema;
    protected DatabaseResp databaseResp;

    protected Agent getAgentByName(String agentName) {
        Optional<Agent> agent = agentService.getAgents().stream()
                .filter(a -> a.getName().equals(agentName)).findFirst();

        return agent.orElse(null);
    }

    protected SemanticQueryResp queryBySql(String sql) throws Exception {
        return queryBySql(sql, User.getDefaultUser());
    }

    protected SemanticQueryResp queryBySql(String sql, User user) throws Exception {
        return semanticLayerService.queryByReq(buildQuerySqlReq(sql), user);
    }

    protected void executeSql(String sql) {
        if (databaseResp == null) {
            databaseResp = databaseService.getDatabase(1L);
        }
        SemanticQueryResp queryResp = databaseService.executeSql(sql, databaseResp);
        assert StringUtils.isBlank(queryResp.getErrorMsg());
        System.out.println(
                String.format("Execute result: %s", JsonUtil.toString(queryResp.getResultList())));
    }

    protected SemanticQueryReq buildQuerySqlReq(String sql) {
        QuerySqlReq querySqlCmd = new QuerySqlReq();
        querySqlCmd.setSql(sql);
        querySqlCmd.setModelIds(DataUtils.getMetricAgentIModelIds());
        return querySqlCmd;
    }

    protected QueryStructReq buildQueryStructReq(List<String> groups) {
        return buildQueryStructReq(groups, QueryType.AGGREGATE);
    }

    protected QueryStructReq buildQueryStructReq(List<String> groups, QueryType queryType) {
        QueryStructReq queryStructReq = new QueryStructReq();
        for (Long modelId : DataUtils.getMetricAgentIModelIds()) {
            queryStructReq.addModelId(modelId);
        }
        queryStructReq.setQueryType(queryType);
        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.SUM);
        aggregator.setColumn("stay_hours");
        queryStructReq.setAggregators(Arrays.asList(aggregator));

        if (CollectionUtils.isNotEmpty(groups)) {
            queryStructReq.setGroups(groups);
        }

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateMode.BETWEEN);
        dateConf.setEndDate(now().plusDays(0).toString());
        dateConf.setStartDate(now().plusDays(-365).toString());
        dateConf.setDateField("imp_date");
        queryStructReq.setDateInfo(dateConf);

        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setColumn("stay_hours");
        orders.add(order);
        queryStructReq.setOrders(orders);
        return queryStructReq;
    }

    protected QueryStructReq buildQueryStructReq(List<String> groups, Aggregator aggregator) {
        QueryStructReq queryStructReq = new QueryStructReq();
        for (Long modelId : DataUtils.getMetricAgentIModelIds()) {
            queryStructReq.addModelId(modelId);
        }
        queryStructReq.setQueryType(QueryType.AGGREGATE);
        queryStructReq.setAggregators(Arrays.asList(aggregator));

        if (CollectionUtils.isNotEmpty(groups)) {
            queryStructReq.setGroups(groups);
        }

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateMode.BETWEEN);
        dateConf.setEndDate(now().plusDays(0).toString());
        dateConf.setStartDate(now().plusDays(-365).toString());
        queryStructReq.setDateInfo(dateConf);
        return queryStructReq;
    }

    protected void setDomainNotOpenToAll() {
        Long s2Domain = 1L;
        DomainDO domainDO = domainRepository.getDomainById(s2Domain);
        domainDO.setIsOpen(0);
        domainRepository.updateDomain(domainDO);
    }
}
