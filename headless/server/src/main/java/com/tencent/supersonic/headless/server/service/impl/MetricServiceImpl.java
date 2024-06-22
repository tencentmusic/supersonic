package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.MetricDOMapper;
import com.tencent.supersonic.headless.server.utils.AliasGenerateHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.MeasureParam;
import com.tencent.supersonic.headless.api.pojo.MetricParam;
import com.tencent.supersonic.headless.api.pojo.MetricQueryDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricBaseReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMapReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetMapInfo;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.TagItem;
import com.tencent.supersonic.headless.server.persistence.dataobject.CollectDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricQueryDefaultConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.persistence.repository.MetricRepository;
import com.tencent.supersonic.headless.server.pojo.DimensionsFilter;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.MetricFilter;
import com.tencent.supersonic.headless.server.pojo.MetricsFilter;
import com.tencent.supersonic.headless.server.pojo.ModelCluster;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import com.tencent.supersonic.headless.server.service.CollectService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.RetrieveService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.TagMetaService;
import com.tencent.supersonic.headless.server.utils.MetricCheckUtils;
import com.tencent.supersonic.headless.server.utils.MetricConverter;
import com.tencent.supersonic.headless.server.utils.ModelClusterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MetricServiceImpl extends ServiceImpl<MetricDOMapper, MetricDO>
        implements MetricService {

    private MetricRepository metricRepository;

    private ModelService modelService;

    private DimensionService dimensionService;

    private AliasGenerateHelper aliasGenerateHelper;

    private CollectService collectService;

    private DataSetService dataSetService;

    private ApplicationEventPublisher eventPublisher;

    private TagMetaService tagMetaService;

    private RetrieveService metaDiscoveryService;

    public MetricServiceImpl(MetricRepository metricRepository,
            ModelService modelService,
            AliasGenerateHelper aliasGenerateHelper,
            CollectService collectService,
            DataSetService dataSetService,
            ApplicationEventPublisher eventPublisher,
            DimensionService dimensionService,
            TagMetaService tagMetaService,
            @Lazy RetrieveService metaDiscoveryService) {
        this.metricRepository = metricRepository;
        this.modelService = modelService;
        this.aliasGenerateHelper = aliasGenerateHelper;
        this.eventPublisher = eventPublisher;
        this.collectService = collectService;
        this.dataSetService = dataSetService;
        this.dimensionService = dimensionService;
        this.tagMetaService = tagMetaService;
        this.metaDiscoveryService = metaDiscoveryService;
    }

    @Override
    public MetricResp createMetric(MetricReq metricReq, User user) {
        checkExist(Lists.newArrayList(metricReq));
        MetricCheckUtils.checkParam(metricReq);
        metricReq.createdBy(user.getName());
        MetricDO metricDO = MetricConverter.convert2MetricDO(metricReq);
        metricRepository.createMetric(metricDO);
        sendEventBatch(Lists.newArrayList(metricDO), EventType.ADD);
        return MetricConverter.convert2MetricResp(metricDO);
    }

    @Override
    public void createMetricBatch(List<MetricReq> metricReqs, User user) {
        if (CollectionUtils.isEmpty(metricReqs)) {
            return;
        }
        Long modelId = metricReqs.get(0).getModelId();
        List<MetricResp> metricResps = getMetrics(new MetaFilter(Lists.newArrayList(modelId)));
        Map<String, MetricResp> bizNameMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
        Map<String, MetricResp> nameMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getName, a -> a, (k1, k2) -> k1));
        List<MetricReq> metricToInsert = metricReqs.stream()
                .filter(metric -> !bizNameMap.containsKey(metric.getBizName())
                        && !nameMap.containsKey(metric.getName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(metricToInsert)) {
            return;
        }
        List<MetricDO> metricDOS = metricToInsert.stream().peek(metric -> metric.createdBy(user.getName()))
                .map(MetricConverter::convert2MetricDO).collect(Collectors.toList());
        metricRepository.createMetricBatch(metricDOS);
        sendEventBatch(metricDOS, EventType.ADD);
    }

    @Override
    public MetricResp updateMetric(MetricReq metricReq, User user) {
        MetricCheckUtils.checkParam(metricReq);
        checkExist(Lists.newArrayList(metricReq));
        metricReq.updatedBy(user.getName());
        MetricDO metricDO = metricRepository.getMetricById(metricReq.getId());
        String oldName = metricDO.getName();
        MetricConverter.convert(metricDO, metricReq);
        metricRepository.updateMetric(metricDO);
        if (!oldName.equals(metricDO.getName())) {
            DataItem dataItem = getDataItem(metricDO);
            dataItem.setName(oldName);
            dataItem.setNewName(metricDO.getName());
            sendEvent(dataItem, EventType.UPDATE);
        }
        return MetricConverter.convert2MetricResp(metricDO);
    }

    @Override
    public void batchUpdateStatus(MetaBatchReq metaBatchReq, User user) {
        if (CollectionUtils.isEmpty(metaBatchReq.getIds())) {
            return;
        }
        List<MetricDO> metricDOS = getMetrics(metaBatchReq.getIds());
        if (CollectionUtils.isEmpty(metricDOS)) {
            return;
        }
        metricDOS = metricDOS.stream()
                .peek(metricDO -> {
                    metricDO.setStatus(metaBatchReq.getStatus());
                    metricDO.setUpdatedAt(new Date());
                    metricDO.setUpdatedBy(user.getName());
                })
                .collect(Collectors.toList());
        metricRepository.batchUpdateStatus(metricDOS);
        if (StatusEnum.OFFLINE.getCode().equals(metaBatchReq.getStatus())
                || StatusEnum.DELETED.getCode().equals(metaBatchReq.getStatus())) {
            sendEventBatch(metricDOS, EventType.DELETE);
        } else if (StatusEnum.ONLINE.getCode().equals(metaBatchReq.getStatus())) {
            sendEventBatch(metricDOS, EventType.ADD);
        }
    }

    @Override
    public void batchPublish(List<Long> metricIds, User user) {
        List<MetricDO> metrics = getMetrics(metricIds);
        for (MetricDO metricDO : metrics) {
            metricDO.setUpdatedAt(new Date());
            metricDO.setUpdatedBy(user.getName());
        }
        metricRepository.batchPublish(metrics);
    }

    @Override
    public void batchUnPublish(List<Long> metricIds, User user) {
        List<MetricDO> metrics = getMetrics(metricIds);
        for (MetricDO metricDO : metrics) {
            metricDO.setUpdatedAt(new Date());
            metricDO.setUpdatedBy(user.getName());
        }
        metricRepository.batchUnPublish(metrics);
    }

    @Override
    public void batchUpdateClassifications(MetaBatchReq metaBatchReq, User user) {
        List<MetricDO> metrics = getMetrics(metaBatchReq.getIds());
        for (MetricDO metricDO : metrics) {
            metricDO.setUpdatedAt(new Date());
            metricDO.setUpdatedBy(user.getName());
            fillClassifications(metaBatchReq, metricDO);
        }
        metricRepository.updateClassificationsBatch(metrics);
    }

    private void fillClassifications(MetaBatchReq metaBatchReq, MetricDO metricDO) {
        String classificationStr = metricDO.getClassifications();
        Set<String> classificationsList;
        if (StringUtils.isBlank(classificationStr)) {
            classificationsList = new HashSet<>();
        } else {
            classificationsList = new HashSet<>(Arrays.asList(classificationStr.split(",")));
        }

        if (EventType.ADD.equals(metaBatchReq.getType())) {
            classificationsList.addAll(metaBatchReq.getClassifications());
        }
        if (EventType.DELETE.equals(metaBatchReq.getType())) {
            classificationsList.removeAll(metaBatchReq.getClassifications());
        }
        String classifications = "";
        if (!CollectionUtils.isEmpty(classificationsList)) {
            classifications = StringUtils.join(classificationsList, ",");
        }
        metricDO.setClassifications(classifications);
    }

    @Override
    public void batchUpdateSensitiveLevel(MetaBatchReq metaBatchReq, User user) {
        List<MetricDO> metrics = getMetrics(metaBatchReq.getIds());
        for (MetricDO metricDO : metrics) {
            metricDO.setUpdatedAt(new Date());
            metricDO.setUpdatedBy(user.getName());
            metricDO.setSensitiveLevel(metaBatchReq.getSensitiveLevel());
        }
        updateBatchById(metrics);
    }

    @Override
    public void deleteMetric(Long id, User user) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            throw new RuntimeException(String.format("the metric %s not exist", id));
        }
        metricDO.setStatus(StatusEnum.DELETED.getCode());
        metricDO.setUpdatedAt(new Date());
        metricDO.setUpdatedBy(user.getName());
        metricRepository.updateMetric(metricDO);
        sendEventBatch(Lists.newArrayList(metricDO), EventType.DELETE);
    }

    @Override
    public PageInfo<MetricResp> queryMetricMarket(PageMetricReq pageMetricReq, User user) {
        //search by whole text
        PageInfo<MetricResp> metricRespPageInfo = queryMetric(pageMetricReq, user);
        if (metricRespPageInfo.hasContent() || StringUtils.isBlank(pageMetricReq.getKey())) {
            return metricRespPageInfo;
        }
        //search by text split
        QueryMapReq queryMapReq = new QueryMapReq();
        queryMapReq.setQueryText(pageMetricReq.getKey());
        queryMapReq.setUser(user);
        queryMapReq.setMapModeEnum(MapModeEnum.LOOSE);
        MapInfoResp mapMeta = metaDiscoveryService.map(queryMapReq);
        Map<String, DataSetMapInfo> dataSetMapInfoMap = mapMeta.getDataSetMapInfo();
        if (CollectionUtils.isEmpty(dataSetMapInfoMap)) {
            return metricRespPageInfo;
        }
        Map<Long, Double> result = dataSetMapInfoMap.values().stream()
                .map(DataSetMapInfo::getMapFields)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream).filter(schemaElementMatch ->
                        SchemaElementType.METRIC.equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toMap(schemaElementMatch ->
                                schemaElementMatch.getElement().getId(), SchemaElementMatch::getSimilarity,
                        (existingValue, newValue) -> existingValue));
        List<Long> metricIds = new ArrayList<>(result.keySet());
        if (CollectionUtils.isEmpty(result.keySet())) {
            return metricRespPageInfo;
        }
        pageMetricReq.setIds(metricIds);
        pageMetricReq.setKey("");
        PageInfo<MetricResp> metricPage = queryMetric(pageMetricReq, user);
        for (MetricResp metricResp : metricPage.getList()) {
            metricResp.setSimilarity(result.get(metricResp.getId()));
        }
        metricPage.getList().sort(Comparator.comparingDouble(MetricResp::getSimilarity).reversed());
        return metricPage;
    }

    @Override
    public PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user) {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setUserName(user.getName());
        BeanUtils.copyProperties(pageMetricReq, metricFilter);
        if (!CollectionUtils.isEmpty(pageMetricReq.getDomainIds())) {
            List<ModelResp> modelResps = modelService.getAllModelByDomainIds(pageMetricReq.getDomainIds());
            List<Long> modelIds = modelResps.stream().map(ModelResp::getId).collect(Collectors.toList());
            pageMetricReq.getModelIds().addAll(modelIds);
        }
        metricFilter.setModelIds(pageMetricReq.getModelIds());
        List<Long> collectIds = getCollectIds(pageMetricReq, user);
        List<Long> idsToFilter = getIdsToFilter(pageMetricReq, collectIds);
        metricFilter.setIds(idsToFilter);
        PageInfo<MetricDO> metricDOPageInfo = PageHelper.startPage(pageMetricReq.getCurrent(),
                        pageMetricReq.getPageSize())
                .doSelectPageInfo(() -> queryMetric(metricFilter));
        PageInfo<MetricResp> pageInfo = new PageInfo<>();
        BeanUtils.copyProperties(metricDOPageInfo, pageInfo);
        List<MetricResp> metricResps = convertList(metricDOPageInfo.getList(), collectIds);
        fillAdminRes(metricResps, user);
        fillTagInfo(metricResps);
        pageInfo.setList(metricResps);
        return pageInfo;
    }

    protected List<MetricDO> queryMetric(MetricFilter metricFilter) {
        return metricRepository.getMetric(metricFilter);
    }

    private List<MetricDO> getMetrics(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Lists.newArrayList();
        }
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setIds(ids);
        return metricRepository.getMetric(metricFilter);
    }

    @Override
    public List<MetricResp> getMetrics(MetaFilter metaFilter) {
        MetricFilter metricFilter = new MetricFilter();
        BeanUtils.copyProperties(metaFilter, metricFilter);
        List<MetricResp> metricResps = convertList(queryMetric(metricFilter));
        List<Long> metricIds = metricResps.stream().map(metricResp -> metricResp.getId()).collect(Collectors.toList());

        List<TagItem> tagItems = tagMetaService.getTagItems(metricIds, TagDefineType.METRIC);
        Map<Long, TagItem> itemIdToTagItem = tagItems.stream()
                .collect(Collectors.toMap(tag -> tag.getItemId(), tag -> tag, (newTag, oldTag) -> newTag));

        if (Objects.nonNull(itemIdToTagItem)) {
            metricResps.stream().forEach(metricResp -> {
                Long metricRespId = metricResp.getId();
                if (itemIdToTagItem.containsKey(metricRespId)) {
                    metricResp.setIsTag(itemIdToTagItem.get(metricRespId).getIsTag());
                }
            });
        }
        if (!CollectionUtils.isEmpty(metaFilter.getFieldsDepend())) {
            return filterByField(metricResps, metaFilter.getFieldsDepend());
        }
        if (metaFilter.getDataSetId() != null) {
            DataSetResp dataSetResp = dataSetService.getDataSet(metaFilter.getDataSetId());
            return MetricConverter.filterByDataSet(metricResps, dataSetResp);
        }
        return metricResps;
    }

    private List<Long> getCollectIds(PageMetricReq pageMetricReq, User user) {
        List<CollectDO> collectList = collectService.getCollectList(user.getName(), TypeEnums.METRIC);
        List<Long> collectIds = collectList.stream().map(CollectDO::getCollectId).collect(Collectors.toList());
        if (pageMetricReq.isHasCollect()) {
            if (CollectionUtils.isEmpty(collectIds)) {
                return Lists.newArrayList(-1L);
            } else {
                return collectIds;
            }
        }
        return Lists.newArrayList();
    }

    private List<Long> getIdsToFilter(PageMetricReq pageMetricReq, List<Long> collectIds) {
        if (CollectionUtils.isEmpty(pageMetricReq.getIds())) {
            return collectIds;
        }
        if (CollectionUtils.isEmpty(collectIds)) {
            return pageMetricReq.getIds();
        }
        List<Long> idsToFilter = new ArrayList<>(collectIds);
        idsToFilter.retainAll(pageMetricReq.getIds());
        idsToFilter.add(-1L);
        return idsToFilter;
    }

    private void fillTagInfo(List<MetricResp> metricRespList) {
        if (CollectionUtils.isEmpty(metricRespList)) {
            return;
        }

        TagFilter tagFilter = new TagFilter();
        tagFilter.setTagDefineType(TagDefineType.METRIC);
        List<Long> metricIds = metricRespList.stream().map(metric -> metric.getId())
                .collect(Collectors.toList());
        tagFilter.setItemIds(metricIds);
        Map<Long, TagDO> keyAndTagMap = tagMetaService.getTagDOList(tagFilter).stream()
                .collect(Collectors.toMap(tag -> tag.getItemId(), tag -> tag,
                        (newTag, oldTag) -> newTag));
        if (Objects.nonNull(keyAndTagMap)) {
            metricRespList.stream().forEach(metric -> {
                if (keyAndTagMap.containsKey(metric.getId())) {
                    metric.setIsTag(1);
                } else {
                    metric.setIsTag(0);
                }
            });
        }
    }

    private List<MetricResp> filterByField(List<MetricResp> metricResps, List<String> fields) {
        Set<MetricResp> metricRespFiltered = Sets.newHashSet();
        for (MetricResp metricResp : metricResps) {
            filterByField(metricResps, metricResp, fields, metricRespFiltered);
        }
        return new ArrayList<>(metricRespFiltered);
    }

    private boolean filterByField(List<MetricResp> metricResps, MetricResp metricResp,
            List<String> fields, Set<MetricResp> metricRespFiltered) {
        if (MetricDefineType.METRIC.equals(metricResp.getMetricDefineType())) {
            List<Long> ids = metricResp.getMetricDefineByMetricParams().getMetrics()
                    .stream().map(MetricParam::getId).collect(Collectors.toList());
            List<MetricResp> metricById = metricResps.stream()
                    .filter(metric -> ids.contains(metric.getId()))
                    .collect(Collectors.toList());
            for (MetricResp metric : metricById) {
                if (filterByField(metricResps, metric, fields, metricRespFiltered)) {
                    metricRespFiltered.add(metricResp);
                    return true;
                }
            }
        } else if (MetricDefineType.FIELD.equals(metricResp.getMetricDefineType())) {
            if (fields.stream().anyMatch(field -> metricResp.getExpr().contains(field))) {
                metricRespFiltered.add(metricResp);
                return true;
            }
        } else if (MetricDefineType.MEASURE.equals(metricResp.getMetricDefineType())) {
            List<MeasureParam> measures = metricResp.getMetricDefineByMeasureParams().getMeasures();
            List<String> fieldNameDepended = measures.stream().map(MeasureParam::getBizName)
                    //measure bizName = model bizName_fieldName
                    .map(name -> name.replaceFirst(metricResp.getModelBizName() + "_", ""))
                    .collect(Collectors.toList());
            if (fields.stream().anyMatch(fieldNameDepended::contains)) {
                metricRespFiltered.add(metricResp);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<MetricResp> getMetricsToCreateNewMetric(Long modelId) {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setModelIds(Lists.newArrayList(modelId));
        List<MetricResp> metricResps = getMetrics(metricFilter);
        return metricResps.stream().filter(metricResp ->
                        MetricDefineType.FIELD.equals(metricResp.getMetricDefineType())
                                || MetricDefineType.MEASURE.equals(metricResp.getMetricDefineType()))
                .collect(Collectors.toList());
    }

    private void fillAdminRes(List<MetricResp> metricResps, User user) {
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        if (CollectionUtils.isEmpty(modelResps)) {
            return;
        }
        Set<Long> modelIdSet = modelResps.stream().map(ModelResp::getId).collect(Collectors.toSet());
        for (MetricResp metricResp : metricResps) {
            if (modelIdSet.contains(metricResp.getModelId())) {
                metricResp.setHasAdminRes(true);
            }
        }
    }

    @Deprecated
    @Override
    public MetricResp getMetric(Long modelId, String bizName) {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setBizName(bizName);
        metricFilter.setModelIds(Lists.newArrayList(modelId));
        List<MetricResp> metricResps = getMetrics(metricFilter);
        MetricResp metricResp = null;
        if (CollectionUtils.isEmpty(metricResps)) {
            return metricResp;
        }
        return metricResps.get(0);
    }

    @Override
    public MetricResp getMetric(Long id, User user) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            return null;
        }
        ModelFilter modelFilter = new ModelFilter(false,
                Lists.newArrayList(metricDO.getModelId()));
        Map<Long, ModelResp> modelMap = modelService.getModelMap(modelFilter);
        List<CollectDO> collectList = collectService.getCollectList(user.getName(), TypeEnums.METRIC);
        List<Long> collect = collectList.stream().map(CollectDO::getCollectId).collect(Collectors.toList());
        MetricResp metricResp = MetricConverter.convert2MetricResp(metricDO, modelMap, collect);
        fillAdminRes(Lists.newArrayList(metricResp), user);
        return metricResp;
    }

    @Override
    public MetricResp getMetric(Long id) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            return null;
        }
        return MetricConverter.convert2MetricResp(metricDO, new HashMap<>(), Lists.newArrayList());
    }

    @Override
    public List<String> mockAlias(MetricBaseReq metricReq, String mockType, User user) {

        String mockAlias = aliasGenerateHelper.generateAlias(mockType, metricReq.getName(), metricReq.getBizName(), "",
                metricReq.getDescription(), !"".equals(metricReq.getDataFormatType()));
        return JSONObject.parseObject(mockAlias, new TypeReference<List<String>>() {
        });
    }

    @Override
    public Set<String> getMetricTags() {
        List<MetricResp> metricResps = getMetrics(new MetaFilter());
        if (CollectionUtils.isEmpty(metricResps)) {
            return new HashSet<>();
        }
        return metricResps.stream().flatMap(metricResp ->
                metricResp.getClassifications().stream()).collect(Collectors.toSet());
    }

    @Override
    public List<DrillDownDimension> getDrillDownDimension(Long metricId) {
        List<DrillDownDimension> drillDownDimensions = Lists.newArrayList();
        MetricResp metricResp = getMetric(metricId);
        if (metricResp == null) {
            return drillDownDimensions;
        }
        if (metricResp.getRelateDimension() != null
                && !CollectionUtils.isEmpty(metricResp.getRelateDimension().getDrillDownDimensions())) {
            for (DrillDownDimension drillDownDimension : metricResp.getRelateDimension().getDrillDownDimensions()) {
                if (drillDownDimension.isInheritedFromModel() && !drillDownDimension.isNecessary()) {
                    continue;
                }
                drillDownDimensions.add(drillDownDimension);
            }
        }
        ModelResp modelResp = modelService.getModel(metricResp.getModelId());
        if (modelResp.getDrillDownDimensions() == null) {
            return drillDownDimensions;
        }
        for (DrillDownDimension drillDownDimension : modelResp.getDrillDownDimensions()) {
            if (!drillDownDimensions.stream().map(DrillDownDimension::getDimensionId)
                    .collect(Collectors.toList()).contains(drillDownDimension.getDimensionId())) {
                drillDownDimension.setInheritedFromModel(true);
                drillDownDimensions.add(drillDownDimension);
            }
        }
        return drillDownDimensions;
    }

    @Override
    public void saveMetricQueryDefaultConfig(MetricQueryDefaultConfig defaultConfig, User user) {
        MetricQueryDefaultConfigDO defaultConfigDO =
                metricRepository.getDefaultQueryConfig(defaultConfig.getMetricId(), user.getName());
        if (defaultConfigDO == null) {
            defaultConfigDO = new MetricQueryDefaultConfigDO();
            defaultConfig.createdBy(user.getName());
            BeanMapper.mapper(defaultConfig, defaultConfigDO);
            metricRepository.saveDefaultQueryConfig(defaultConfigDO);
        } else {
            defaultConfig.setId(defaultConfigDO.getId());
            defaultConfig.updatedBy(user.getName());
            BeanMapper.mapper(defaultConfig, defaultConfigDO);
            metricRepository.updateDefaultQueryConfig(defaultConfigDO);
        }
    }

    @Override
    public MetricQueryDefaultConfig getMetricQueryDefaultConfig(Long metricId, User user) {
        MetricQueryDefaultConfigDO metricQueryDefaultConfigDO =
                metricRepository.getDefaultQueryConfig(metricId, user.getName());
        MetricQueryDefaultConfig metricQueryDefaultConfig = new MetricQueryDefaultConfig();
        BeanMapper.mapper(metricQueryDefaultConfigDO, metricQueryDefaultConfig);
        return metricQueryDefaultConfig;
    }

    private void checkExist(List<MetricBaseReq> metricReqs) {
        Long modelId = metricReqs.get(0).getModelId();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setModelIds(Lists.newArrayList(modelId));
        List<MetricResp> metricResps = getMetrics(metaFilter);
        Map<String, MetricResp> bizNameMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
        Map<String, MetricResp> nameMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getName, a -> a, (k1, k2) -> k1));
        for (MetricBaseReq metricReq : metricReqs) {
            if (bizNameMap.containsKey(metricReq.getBizName())) {
                MetricResp metricResp = bizNameMap.get(metricReq.getBizName());
                if (!metricResp.getId().equals(metricReq.getId())) {
                    throw new RuntimeException(String.format("该模型下存在相同的指标字段名:%s 创建人:%s",
                            metricReq.getBizName(), metricResp.getCreatedBy()));
                }
            }
            if (nameMap.containsKey(metricReq.getName())) {
                MetricResp metricResp = nameMap.get(metricReq.getName());
                if (!metricResp.getId().equals(metricReq.getId())) {
                    throw new RuntimeException(String.format("该模型下存在相同的指标名:%s 创建人:%s",
                            metricReq.getName(), metricResp.getCreatedBy()));
                }
            }
        }
    }

    private List<MetricResp> convertList(List<MetricDO> metricDOS) {
        return convertList(metricDOS, Lists.newArrayList());
    }

    private List<MetricResp> convertList(List<MetricDO> metricDOS, List<Long> collect) {
        List<MetricResp> metricResps = Lists.newArrayList();
        List<Long> modelIds = metricDOS.stream().map(MetricDO::getModelId)
                .collect(Collectors.toList());
        ModelFilter modelFilter = new ModelFilter(false, modelIds);
        Map<Long, ModelResp> modelMap = modelService.getModelMap(modelFilter);
        if (!CollectionUtils.isEmpty(metricDOS)) {
            metricResps = metricDOS.stream()
                    .map(metricDO -> MetricConverter.convert2MetricResp(metricDO, modelMap, collect))
                    .collect(Collectors.toList());
        }
        return metricResps;
    }

    @Override
    public void sendMetricEventBatch(List<Long> modelIds, EventType eventType) {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setModelIds(modelIds);
        List<MetricDO> metricDOS = queryMetric(metricFilter);
        sendEventBatch(metricDOS, eventType);
    }

    @Override
    public List<MetricResp> queryMetrics(MetricsFilter metricsFilter) {
        List<MetricDO> metricDOS = metricRepository.getMetrics(metricsFilter);
        return convertList(metricDOS, new ArrayList<>());
    }

    @Override
    public DataEvent getDataEvent() {
        MetricsFilter metricsFilter = new MetricsFilter();
        List<MetricDO> metricDOS = metricRepository.getMetrics(metricsFilter);
        return getDataEvent(metricDOS, EventType.ADD);
    }

    private DataEvent getDataEvent(List<MetricDO> metricDOS, EventType eventType) {
        List<DataItem> dataItems = metricDOS.stream().map(this::getDataItem)
                .collect(Collectors.toList());
        return new DataEvent(this, dataItems, eventType);
    }

    private void sendEventBatch(List<MetricDO> metricDOS, EventType eventType) {
        DataEvent dataEvent = getDataEvent(metricDOS, eventType);
        eventPublisher.publishEvent(dataEvent);
    }

    private void sendEvent(DataItem dataItem, EventType eventType) {
        eventPublisher.publishEvent(new DataEvent(this,
                Lists.newArrayList(dataItem), eventType));
    }

    private DataItem getDataItem(MetricDO metricDO) {
        MetricResp metricResp = MetricConverter.convert2MetricResp(metricDO,
                new HashMap<>(), Lists.newArrayList());
        fillDefaultAgg(metricResp);
        return DataItem.builder().id(metricDO.getId() + Constants.UNDERLINE).name(metricDO.getName())
                .bizName(metricDO.getBizName())
                .modelId(metricDO.getModelId() + Constants.UNDERLINE)
                .type(TypeEnums.METRIC).defaultAgg(metricResp.getDefaultAgg()).build();
    }

    @Override
    public void batchFillMetricDefaultAgg(List<MetricResp> metricResps, List<ModelResp> modelResps) {
        Map<Long, ModelResp> modelRespMap = modelResps.stream().collect(Collectors.toMap(ModelResp::getId, m -> m));
        for (MetricResp metricResp : metricResps) {
            fillDefaultAgg(metricResp, modelRespMap.get(metricResp.getModelId()));
        }
    }

    private void fillDefaultAgg(MetricResp metricResp) {
        if (MetricDefineType.MEASURE.equals(metricResp.getMetricDefineType())) {
            Long modelId = metricResp.getModelId();
            ModelResp modelResp = modelService.getModel(modelId);
            fillDefaultAgg(metricResp, modelResp);
        }
    }

    private void fillDefaultAgg(MetricResp metricResp, ModelResp modelResp) {
        metricResp.setDefaultAgg(getDefaultAgg(metricResp, modelResp));
    }

    private String getDefaultAgg(MetricResp metricResp, ModelResp modelResp) {
        if (modelResp == null || (Objects.nonNull(metricResp.getDefaultAgg()) && !metricResp.getDefaultAgg()
                .isEmpty())) {
            return metricResp.getDefaultAgg();
        }
        // FIELD define will get from expr
        if (MetricDefineType.FIELD.equals(metricResp.getMetricDefineType())) {
            return SqlSelectFunctionHelper.getFirstAggregateFunctions(metricResp.getExpr());
        }
        // METRIC define will get from first metric
        if (MetricDefineType.METRIC.equals(metricResp.getMetricDefineType())) {
            if (!CollectionUtils.isEmpty(
                    metricResp.getMetricDefineByMetricParams().getMetrics())) {
                MetricParam metricParam = metricResp.getMetricDefineByMetricParams().getMetrics().get(0);
                MetricResp firstMetricResp = getMetric(modelResp.getDomainId(), metricParam.getBizName());
                if (Objects.nonNull(firstMetricResp)) {
                    return getDefaultAgg(firstMetricResp, modelResp);
                }
                return "";
            }
        }
        // Measure define will get from first measure
        List<Measure> measures = modelResp.getModelDetail().getMeasures();
        MeasureParam firstMeasure = metricResp.getMetricDefineByMeasureParams()
                .getMeasures().get(0);
        for (Measure measure : measures) {
            if (measure.getBizName().equalsIgnoreCase(firstMeasure.getBizName())) {
                return measure.getAgg();
            }
        }
        return "";
    }

    @Override
    public QueryStructReq convert(QueryMetricReq queryMetricReq) {
        //1. If a domainId exists, the modelIds obtained from the domainId.
        Set<Long> modelIdsByDomainId = getModelIdsByDomainId(queryMetricReq);

        //2. get metrics and dimensions
        List<MetricResp> metricResps = getMetricResps(queryMetricReq, modelIdsByDomainId);

        List<DimensionResp> dimensionResps = getDimensionResps(modelIdsByDomainId);
        Map<Long, DimensionResp> dimensionRespMap = dimensionResps.stream()
                .collect(Collectors.toMap(DimensionResp::getId, d -> d));

        //3. choose ModelCluster
        Set<Long> modelIds = getModelIds(modelIdsByDomainId, metricResps, dimensionResps);
        ModelCluster modelCluster = getModelCluster(metricResps, modelIds);
        if (modelCluster == null) {
            throw new IllegalArgumentException("Invalid input parameters, unable to obtain valid metrics");
        }
        //4. set groups
        List<String> dimensionBizNames = dimensionResps.stream()
                .filter(entry -> modelCluster.getModelIds().contains(entry.getModelId()))
                .filter(entry -> queryMetricReq.getDimensionNames().contains(entry.getName())
                        || queryMetricReq.getDimensionNames().contains(entry.getBizName())
                        || queryMetricReq.getDimensionIds().contains(entry.getId()))
                .map(SchemaItem::getBizName).collect(Collectors.toList());

        QueryStructReq queryStructReq = new QueryStructReq();
        if (queryMetricReq.getDateInfo().isGroupByDate()) {
            queryStructReq.getGroups().add(queryMetricReq.getDateInfo().getGroupByTimeDimension());
        }
        if (!CollectionUtils.isEmpty(dimensionBizNames)) {
            queryStructReq.getGroups().addAll(dimensionBizNames);
        }
        //5. set aggregators
        List<String> metricBizNames = metricResps.stream()
                .filter(entry -> modelCluster.getModelIds().contains(entry.getModelId()))
                .map(SchemaItem::getBizName).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(metricBizNames)) {
            throw new IllegalArgumentException("Invalid input parameters, unable to obtain valid metrics");
        }
        List<Aggregator> aggregators = new ArrayList<>();
        for (String metricBizName : metricBizNames) {
            Aggregator aggregator = new Aggregator();
            aggregator.setColumn(metricBizName);
            aggregators.add(aggregator);
        }
        queryStructReq.setAggregators(aggregators);
        queryStructReq.setLimit(queryMetricReq.getLimit());
        //6. set modelIds
        for (Long modelId : modelCluster.getModelIds()) {
            queryStructReq.addModelId(modelId);
        }
        List<Filter> filters = queryMetricReq.getFilters();
        for (Filter filter : filters) {
            if (StringUtils.isBlank(filter.getBizName())) {
                DimensionResp dimensionResp = dimensionRespMap.get(filter.getId());
                if (dimensionResp != null) {
                    filter.setBizName(dimensionResp.getBizName());
                }
            }
        }
        queryStructReq.setDimensionFilters(filters);
        //7. set dateInfo
        queryStructReq.setDateInfo(queryMetricReq.getDateInfo());
        return queryStructReq;
    }

    private ModelCluster getModelCluster(List<MetricResp> metricResps, Set<Long> modelIds) {
        Map<String, ModelCluster> modelClusterMap = ModelClusterBuilder.buildModelClusters(new ArrayList<>(modelIds));

        Map<String, List<SchemaItem>> modelClusterToMatchCount = new HashMap<>();
        for (ModelCluster modelCluster : modelClusterMap.values()) {
            for (MetricResp metricResp : metricResps) {
                if (modelCluster.getModelIds().contains(metricResp.getModelId())) {
                    modelClusterToMatchCount.computeIfAbsent(modelCluster.getKey(), k -> new ArrayList<>())
                            .add(metricResp);
                }
            }
        }
        String keyWithMaxSize = modelClusterToMatchCount.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);

        return modelClusterMap.get(keyWithMaxSize);
    }

    private Set<Long> getModelIds(Set<Long> modelIdsByDomainId, List<MetricResp> metricResps,
            List<DimensionResp> dimensionResps) {
        Set<Long> result = new HashSet<>();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(modelIdsByDomainId)) {
            result.addAll(modelIdsByDomainId);
            return result;
        }
        Set<Long> metricModelIds = metricResps.stream().map(entry -> entry.getModelId())
                .collect(Collectors.toSet());
        result.addAll(metricModelIds);

        Set<Long> dimensionModelIds = dimensionResps.stream().map(entry -> entry.getModelId())
                .collect(Collectors.toSet());
        result.addAll(dimensionModelIds);
        return result;
    }

    private List<DimensionResp> getDimensionResps(Set<Long> modelIds) {
        DimensionsFilter dimensionsFilter = new DimensionsFilter();
        dimensionsFilter.setModelIds(new ArrayList<>(modelIds));
        return dimensionService.queryDimensions(dimensionsFilter);
    }

    private List<MetricResp> getMetricResps(QueryMetricReq queryMetricReq, Set<Long> modelIds) {
        MetricsFilter metricsFilter = new MetricsFilter();
        BeanUtils.copyProperties(queryMetricReq, metricsFilter);
        metricsFilter.setModelIds(new ArrayList<>(modelIds));
        return queryMetrics(metricsFilter);
    }

    private Set<Long> getModelIdsByDomainId(QueryMetricReq queryMetricReq) {
        List<ModelResp> modelResps = modelService.getAllModelByDomainIds(
                Collections.singletonList(queryMetricReq.getDomainId()));
        return modelResps.stream().map(ModelResp::getId).collect(Collectors.toSet());
    }

}
