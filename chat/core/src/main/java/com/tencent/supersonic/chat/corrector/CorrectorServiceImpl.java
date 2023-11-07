package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.query.request.QueryS2QLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CorrectorServiceImpl implements CorrectorService {

    @Autowired
    private SchemaService schemaService;

    public SemanticCorrectInfo correctorSql(QueryFilters queryFilters, SemanticParseInfo parseInfo, String sql) {

        SemanticCorrectInfo correctInfo = SemanticCorrectInfo.builder()
                .queryFilters(queryFilters).sql(sql)
                .parseInfo(parseInfo).build();

        List<SemanticCorrector> corrections = ComponentFactory.getSqlCorrections();

        corrections.forEach(correction -> {
            try {
                correction.correct(correctInfo);
                log.info("sqlCorrection:{} sql:{}", correction.getClass().getSimpleName(), correctInfo.getSql());
            } catch (Exception e) {
                log.error(String.format("correct error,correctInfo:%s", correctInfo), e);
            }
        });
        return correctInfo;
    }


    public void addS2QLAndLoginSql(QueryStructReq queryStructReq, SemanticParseInfo parseInfo) {
        convertBizNameToName(queryStructReq, parseInfo);
        QueryS2QLReq queryS2QLReq = queryStructReq.convert(queryStructReq);
        parseInfo.getSqlInfo().setS2QL(queryS2QLReq.getSql());
        queryStructReq.setS2QL(queryS2QLReq.getSql());

        SemanticCorrectInfo semanticCorrectInfo = correctorSql(new QueryFilters(), parseInfo,
                queryS2QLReq.getSql());
        parseInfo.getSqlInfo().setLogicSql(semanticCorrectInfo.getSql());

        queryStructReq.setLogicSql(semanticCorrectInfo.getSql());
    }


    private void convertBizNameToName(QueryStructReq queryStructReq, SemanticParseInfo parseInfo) {
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

}
