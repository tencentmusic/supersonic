
package com.tencent.supersonic.chat.query;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.enums.QueryTypeEnum;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import com.tencent.supersonic.semantic.api.query.request.ExplainSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryS2SQLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@ToString
public abstract class BaseSemanticQuery implements SemanticQuery, Serializable {

    protected SemanticParseInfo parseInfo = new SemanticParseInfo();

    protected SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();

    @Override
    public String explain(User user) {
        ExplainSqlReq explainSqlReq = null;
        try {
            ExplainResp explain = null;
            SqlInfo sqlInfo = parseInfo.getSqlInfo();
            if (StringUtils.isNotBlank(sqlInfo.getCorrectS2SQL())) {
                //sql
                QueryS2SQLReq queryS2SQLReq = QueryReqBuilder.buildS2SQLReq(sqlInfo.getCorrectS2SQL(),
                        parseInfo.getModelId());
                explainSqlReq = ExplainSqlReq.builder()
                        .queryTypeEnum(QueryTypeEnum.SQL)
                        .queryReq(queryS2SQLReq)
                        .build();
                explain = semanticInterpreter.explain(explainSqlReq, user);
            } else {
                //struct
                QueryStructReq queryStructReq = QueryReqBuilder.buildStructReq(parseInfo);
                explainSqlReq = ExplainSqlReq.builder()
                        .queryTypeEnum(QueryTypeEnum.STRUCT)
                        .queryReq(queryStructReq)
                        .build();
                explain = semanticInterpreter.explain(explainSqlReq, user);
            }
            if (Objects.nonNull(explain)) {
                return explain.getSql();
            }
        } catch (Exception e) {
            log.error("explain error explainSqlReq:{}", explainSqlReq, e);
        }
        return null;
    }

    @Override
    public SemanticParseInfo getParseInfo() {
        return parseInfo;
    }

    @Override
    public void setParseInfo(SemanticParseInfo parseInfo) {
        this.parseInfo = parseInfo;
    }

    protected QueryStructReq convertQueryStruct() {
        return QueryReqBuilder.buildStructReq(parseInfo);
    }

    protected void convertBizNameToName(QueryStructReq queryStructReq) {
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        Map<String, String> bizNameToName = schemaService.getSemanticSchema()
                .getBizNameToName(queryStructReq.getModelId());
        List<Order> orders = queryStructReq.getOrders();
        if (CollectionUtils.isNotEmpty(orders)) {
            for (Order order : orders) {
                order.setColumn(bizNameToName.get(order.getColumn()));
            }
        }
        List<Aggregator> aggregators = queryStructReq.getAggregators();
        if (CollectionUtils.isNotEmpty(aggregators)) {
            for (Aggregator aggregator : aggregators) {
                aggregator.setColumn(bizNameToName.get(aggregator.getColumn()));
            }
        }
        List<String> groups = queryStructReq.getGroups();
        if (CollectionUtils.isNotEmpty(groups)) {
            groups = groups.stream().map(group -> bizNameToName.get(group)).collect(Collectors.toList());
            queryStructReq.setGroups(groups);
        }
        List<Filter> dimensionFilters = queryStructReq.getDimensionFilters();
        if (CollectionUtils.isNotEmpty(dimensionFilters)) {
            dimensionFilters.stream().forEach(filter -> filter.setName(bizNameToName.get(filter.getBizName())));
        }
        List<Filter> metricFilters = queryStructReq.getMetricFilters();
        if (CollectionUtils.isNotEmpty(dimensionFilters)) {
            metricFilters.stream().forEach(filter -> filter.setName(bizNameToName.get(filter.getBizName())));
        }
        queryStructReq.setModelName(parseInfo.getModelName());
    }

    protected void initS2SqlByStruct() {
        OptimizationConfig optimizationConfig = ContextUtils.getBean(OptimizationConfig.class);
        if (!optimizationConfig.isUseS2SqlSwitch()) {
            return;
        }
        QueryStructReq queryStructReq = convertQueryStruct();
        convertBizNameToName(queryStructReq);
        QueryS2SQLReq queryS2SQLReq = queryStructReq.convert(queryStructReq);
        parseInfo.getSqlInfo().setS2SQL(queryS2SQLReq.getSql());
        parseInfo.getSqlInfo().setCorrectS2SQL(queryS2SQLReq.getSql());
    }

}
