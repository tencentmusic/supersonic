
package com.tencent.supersonic.headless.core.chat.query;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.core.config.ParserConfig;
import com.tencent.supersonic.headless.core.utils.QueryReqBuilder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.core.config.ParserConfig.PARSER_S2SQL_ENABLE;

@Slf4j
@ToString
public abstract class BaseSemanticQuery implements SemanticQuery, Serializable {

    protected SemanticParseInfo parseInfo = new SemanticParseInfo();

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

    protected void convertBizNameToName(SemanticSchema semanticSchema, QueryStructReq queryStructReq) {
        Map<String, String> bizNameToName = semanticSchema.getBizNameToName(queryStructReq.getDataSetId());
        bizNameToName.putAll(TimeDimensionEnum.getNameToNameMap());

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
            groups = groups.stream().map(bizNameToName::get).collect(Collectors.toList());
            queryStructReq.setGroups(groups);
        }
        List<Filter> dimensionFilters = queryStructReq.getDimensionFilters();
        if (CollectionUtils.isNotEmpty(dimensionFilters)) {
            dimensionFilters.forEach(filter -> filter.setName(bizNameToName.get(filter.getBizName())));
        }
        List<Filter> metricFilters = queryStructReq.getMetricFilters();
        if (CollectionUtils.isNotEmpty(dimensionFilters)) {
            metricFilters.forEach(filter -> filter.setName(bizNameToName.get(filter.getBizName())));
        }
    }

    protected void initS2SqlByStruct(SemanticSchema semanticSchema) {
        ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
        boolean s2sqlEnable = Boolean.valueOf(parserConfig.getParameterValue(PARSER_S2SQL_ENABLE));
        if (!s2sqlEnable) {
            return;
        }
        QueryStructReq queryStructReq = convertQueryStruct();
        convertBizNameToName(semanticSchema, queryStructReq);
        QuerySqlReq querySQLReq = queryStructReq.convert();
        parseInfo.getSqlInfo().setS2SQL(querySQLReq.getSql());
        parseInfo.getSqlInfo().setCorrectS2SQL(querySQLReq.getSql());
    }

}
