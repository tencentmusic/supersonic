package com.tencent.supersonic.headless.server.service.impl;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.headless.api.request.ItemUseReq;
import com.tencent.supersonic.headless.api.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.api.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.request.PageMetricReq;
import com.tencent.supersonic.headless.api.request.SchemaItemQueryReq;
import com.tencent.supersonic.headless.api.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.api.response.DomainResp;
import com.tencent.supersonic.headless.api.response.ItemUseResp;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.api.response.ModelSchemaResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

@Slf4j
@Service
public class SchemaServiceImpl implements SchemaService {

    private final QueryService queryService;
    private final ModelService modelService;
    private final DimensionService dimensionService;
    private final MetricService metricService;
    private final DomainService domainService;

    public SchemaServiceImpl(QueryService queryService,
                             ModelService modelService,
                             DimensionService dimensionService,
                             MetricService metricService,
                             DomainService domainService) {
        this.queryService = queryService;
        this.modelService = modelService;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.domainService = domainService;
    }

    @Override
    public List<ModelSchemaResp> fetchModelSchema(ModelSchemaFilterReq filter, User user) {
        List<ModelSchemaResp> domainSchemaDescList = modelService.fetchModelSchema(filter);
        ItemUseReq itemUseCommend = new ItemUseReq();
        itemUseCommend.setModelIds(filter.getModelIds());

        List<ItemUseResp> statInfos = queryService.getStatInfo(itemUseCommend);
        log.debug("statInfos:{}", statInfos);
        fillCnt(domainSchemaDescList, statInfos);
        return domainSchemaDescList;
    }

    private void fillCnt(List<ModelSchemaResp> domainSchemaDescList, List<ItemUseResp> statInfos) {

        Map<String, ItemUseResp> typeIdAndStatPair = statInfos.stream()
                .collect(Collectors.toMap(
                        itemUseInfo -> itemUseInfo.getType() + AT_SYMBOL + AT_SYMBOL + itemUseInfo.getBizName(),
                        itemUseInfo -> itemUseInfo,
                        (item1, item2) -> item1));
        log.debug("typeIdAndStatPair:{}", typeIdAndStatPair);
        for (ModelSchemaResp domainSchemaDesc : domainSchemaDescList) {
            fillDimCnt(domainSchemaDesc, typeIdAndStatPair);
            fillMetricCnt(domainSchemaDesc, typeIdAndStatPair);
        }
    }

    private void fillMetricCnt(ModelSchemaResp domainSchemaDesc, Map<String, ItemUseResp> typeIdAndStatPair) {
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

    private void fillDimCnt(ModelSchemaResp domainSchemaDesc, Map<String, ItemUseResp> typeIdAndStatPair) {
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
    public PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionCmd, User user) {
        return dimensionService.queryDimension(pageDimensionCmd);
    }

    @Override
    public PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user) {
        return metricService.queryMetric(pageMetricReq, user);
    }

    @Override
    public List querySchemaItem(SchemaItemQueryReq schemaItemQueryReq) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setIds(schemaItemQueryReq.getIds());
        if (TypeEnums.METRIC.equals(schemaItemQueryReq.getType())) {
            return metricService.getMetrics(metaFilter);
        } else if (TypeEnums.DIMENSION.equals(schemaItemQueryReq.getType())) {
            return dimensionService.getDimensions(metaFilter);
        }
        throw new InvalidArgumentException("暂不支持的类型" + schemaItemQueryReq.getType().getName());
    }

    @Override
    public List<DomainResp> getDomainList(User user) {
        return domainService.getDomainListWithAdminAuth(user);
    }

    @Override
    public List<ModelResp> getModelList(User user, AuthType authTypeEnum, Long domainId) {
        return modelService.getModelListWithAuth(user, domainId, authTypeEnum);
    }

}
