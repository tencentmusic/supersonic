package com.tencent.supersonic.semantic.query.service;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.ItemUseReq;
import com.tencent.supersonic.semantic.api.query.response.ItemUseResp;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class SchemaServiceImpl implements SchemaService {

    private final QueryService queryService;
    private final DomainService domainService;
    private final DimensionService dimensionService;
    private final MetricService metricService;

    public SchemaServiceImpl(QueryService queryService,
            DomainService domainService,
            DimensionService dimensionService,
            MetricService metricService) {
        this.queryService = queryService;
        this.domainService = domainService;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
    }


    @Override
    public List<DomainSchemaResp> fetchDomainSchema(DomainSchemaFilterReq filter, User user) {
        List<DomainSchemaResp> domainSchemaDescList = domainService.fetchDomainSchema(filter, user);
        List<ItemUseResp> statInfos = queryService.getStatInfo(new ItemUseReq());
        log.debug("statInfos:{}", statInfos);
        fillCnt(domainSchemaDescList, statInfos);
        return domainSchemaDescList;

    }


    private void fillCnt(List<DomainSchemaResp> domainSchemaDescList, List<ItemUseResp> statInfos) {

        Map<String, ItemUseResp> typeIdAndStatPair = statInfos.stream()
                .collect(Collectors.toMap(
                        itemUseInfo -> itemUseInfo.getType() + AT_SYMBOL + AT_SYMBOL + itemUseInfo.getBizName(),
                        itemUseInfo -> itemUseInfo,
                        (item1, item2) -> item1));
        log.debug("typeIdAndStatPair:{}", typeIdAndStatPair);
        for (DomainSchemaResp domainSchemaDesc : domainSchemaDescList) {
            fillDimCnt(domainSchemaDesc, typeIdAndStatPair);
            fillMetricCnt(domainSchemaDesc, typeIdAndStatPair);
        }
    }

    private void fillMetricCnt(DomainSchemaResp domainSchemaDesc, Map<String, ItemUseResp> typeIdAndStatPair) {
        List<MetricSchemaResp> metrics = domainSchemaDesc.getMetrics();
        if (CollectionUtils.isEmpty(domainSchemaDesc.getMetrics())) {
            return;
        }

        if (!CollectionUtils.isEmpty(metrics)) {
            metrics.stream().forEach(metric -> {
                String key = TypeEnums.METRIC.getName() + AT_SYMBOL + AT_SYMBOL + metric.getBizName();
                if (typeIdAndStatPair.containsKey(key)) {
                    metric.setUseCnt(typeIdAndStatPair.get(key).getUseCnt());
                }
            });
        }
        domainSchemaDesc.setMetrics(metrics);
    }

    private void fillDimCnt(DomainSchemaResp domainSchemaDesc, Map<String, ItemUseResp> typeIdAndStatPair) {
        List<DimSchemaResp> dimensions = domainSchemaDesc.getDimensions();
        if (CollectionUtils.isEmpty(domainSchemaDesc.getDimensions())) {
            return;
        }
        if (!CollectionUtils.isEmpty(dimensions)) {
            dimensions.stream().forEach(dim -> {
                String key = TypeEnums.DIMENSION.getName() + AT_SYMBOL + AT_SYMBOL + dim.getBizName();
                if (typeIdAndStatPair.containsKey(key)) {
                    dim.setUseCnt(typeIdAndStatPair.get(key).getUseCnt());
                }
            });
        }
        domainSchemaDesc.setDimensions(dimensions);
    }

    @Override
    public List<DomainResp> getDomainListForAdmin(User user) {
        return domainService.getDomainListForAdmin(user.getName());
    }

    @Override
    public List<DomainResp> getDomainListForViewer(User user) {
        return domainService.getDomainListForViewer(user.getName());
    }

    @Override
    public PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionCmd, User user) {
        return dimensionService.queryDimension(pageDimensionCmd);
    }

    @Override
    public PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricCmd, User user) {
        return metricService.queryMetric(pageMetricCmd);
    }
}
