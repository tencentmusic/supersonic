package com.tencent.supersonic.headless.chat.query;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public SemanticQueryReq buildSemanticQueryReq() {
        return QueryReqBuilder.buildS2SQLReq(parseInfo.getSqlInfo(), parseInfo.getDataSetId());
    }

    protected void initS2SqlByStruct(DataSetSchema dataSetSchema) {
        QueryStructReq queryStructReq = convertQueryStruct();
        convertBizNameToName(dataSetSchema, queryStructReq);
        QuerySqlReq querySQLReq = queryStructReq.convert();
        parseInfo.getSqlInfo().setParsedS2SQL(querySQLReq.getSql());
        parseInfo.getSqlInfo().setCorrectedS2SQL(querySQLReq.getSql());
    }

    protected void convertBizNameToName(DataSetSchema dataSetSchema,
            QueryStructReq queryStructReq) {
        Map<String, String> bizNameToName = dataSetSchema.getBizNameToName();
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
            dimensionFilters
                    .forEach(filter -> filter.setName(bizNameToName.get(filter.getBizName())));
        }
        List<Filter> metricFilters = queryStructReq.getMetricFilters();
        if (CollectionUtils.isNotEmpty(dimensionFilters)) {
            metricFilters.forEach(filter -> filter.setName(bizNameToName.get(filter.getBizName())));
        }
    }
}
