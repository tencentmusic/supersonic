package com.tencent.supersonic.semantic.model.application;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DataAddEvent;
import com.tencent.supersonic.common.pojo.DataDeleteEvent;
import com.tencent.supersonic.common.pojo.DataUpdateEvent;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.ChatGptHelper;
import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.dataobject.MetricDO;
import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.model.domain.repository.MetricRepository;
import com.tencent.supersonic.semantic.model.domain.utils.MetricConverter;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.pojo.Metric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.tencent.supersonic.semantic.model.domain.utils.NameCheckUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class MetricServiceImpl implements MetricService {

    private MetricRepository metricRepository;

    private ModelService modelService;

    private DomainService domainService;

    private ChatGptHelper chatGptHelper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public MetricServiceImpl(MetricRepository metricRepository,
                             ModelService modelService,
                             DomainService domainService,
                             ChatGptHelper chatGptHelper) {
        this.domainService = domainService;
        this.metricRepository = metricRepository;
        this.modelService = modelService;
        this.chatGptHelper = chatGptHelper;
    }

    @Override
    public void creatExprMetric(MetricReq metricReq, User user) {
        check(Lists.newArrayList(metricReq));
        Metric metric = MetricConverter.convert(metricReq);
        metric.createdBy(user.getName());
        log.info("[create metric] object:{}", JSONObject.toJSONString(metric));
        saveMetric(metric);
        //动态更新字典
        String type = DictWordType.METRIC.getType();
        MetricResp metricResp = getMetric(metric.getModelId(), metric.getBizName());
        applicationEventPublisher.publishEvent(
                new DataAddEvent(this, metric.getName(), metric.getModelId(), metricResp.getId(), type));
    }

    @Override
    public void createMetricBatch(List<MetricReq> metricReqs, User user) {
        if (CollectionUtils.isEmpty(metricReqs)) {
            return;
        }
        List<Metric> metrics = metricReqs.stream().map(MetricConverter::convert).collect(Collectors.toList());
        Long modelId = metricReqs.get(0).getModelId();
        List<MetricResp> metricResps = getMetricByModelId(modelId);
        Map<String, MetricResp> metricRespMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
        List<Metric> metricToInsert = metrics.stream()
                .filter(metric -> !metricRespMap.containsKey(metric.getBizName())).collect(Collectors.toList());
        log.info("[insert metric] object:{}", JSONObject.toJSONString(metricToInsert));
        saveMetricBatch(metricToInsert, user);
    }

    @Override
    public List<MetricResp> getMetrics(Long modelId) {
        return convertList(metricRepository.getMetricList(modelId));
    }

    @Override
    public List<MetricResp> getMetrics() {
        return convertList(metricRepository.getMetricList());
    }

    @Override
    public List<MetricResp> getMetrics(Long modelId, Long datasourceId) {
        List<MetricResp> metricResps = convertList(metricRepository.getMetricList(modelId));
        return metricResps.stream().filter(metricResp -> {
            Set<Long> datasourceIdSet = metricResp.getTypeParams().getMeasures().stream()
                    .map(Measure::getDatasourceId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            return !CollectionUtils.isEmpty(datasourceIdSet) && datasourceIdSet.contains(datasourceId);
        }).collect(Collectors.toList());
    }

    public List<MetricResp> getMetrics(List<Long> ids) {
        List<MetricDO> metricDOS = metricRepository.getMetricListByIds(ids);
        return convertList(metricDOS);
    }

    @Override
    public List<MetricResp> getMetricsByModelIds(List<Long> modelIds) {
        List<MetricDO> metricDOS = metricRepository.getMetricList(modelIds);
        return convertList(metricDOS);
    }

    @Override
    public PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user) {
        MetricFilter metricFilter = new MetricFilter();
        BeanUtils.copyProperties(pageMetricReq, metricFilter);
        Set<DomainResp> domainResps = domainService.getDomainChildren(pageMetricReq.getDomainIds());
        List<Long> domainIds = domainResps.stream().map(DomainResp::getId).collect(Collectors.toList());
        List<ModelResp> modelResps = modelService.getModelByDomainIds(domainIds);
        List<Long> modelIds = modelResps.stream().map(ModelResp::getId).collect(Collectors.toList());
        pageMetricReq.getModelIds().addAll(modelIds);
        metricFilter.setModelIds(pageMetricReq.getModelIds());
        PageInfo<MetricDO> metricDOPageInfo = PageHelper.startPage(pageMetricReq.getCurrent(),
                        pageMetricReq.getPageSize())
                .doSelectPageInfo(() -> queryMetric(metricFilter));
        PageInfo<MetricResp> pageInfo = new PageInfo<>();
        BeanUtils.copyProperties(metricDOPageInfo, pageInfo);
        List<MetricResp> metricResps = convertList(metricDOPageInfo.getList());
        fillAdminRes(metricResps, user);
        pageInfo.setList(metricResps);
        return pageInfo;
    }

    private List<MetricDO> queryMetric(MetricFilter metricFilter) {
        return metricRepository.getMetric(metricFilter);
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

    @Override
    public MetricResp getMetric(Long modelId, String bizName) {
        List<MetricResp> metricResps = getMetricByModelId(modelId);
        MetricResp metricResp = null;
        if (CollectionUtils.isEmpty(metricResps)) {
            return metricResp;
        }
        for (MetricResp metric : metricResps) {
            if (metric.getBizName().equalsIgnoreCase(bizName)) {
                metricResp = metric;
            }
        }
        return metricResp;
    }

    private MetricResp getMetric(Long id) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            return null;
        }
        return MetricConverter.convert2MetricResp(metricDO, new HashMap<>());
    }

    @Override
    public void updateExprMetric(MetricReq metricReq, User user) {
        preCheckMetric(metricReq);
        Metric metric = MetricConverter.convert(metricReq);
        metric.updatedBy(user.getName());
        log.info("[update metric] object:{}", JSONObject.toJSONString(metric));
        List<MetricResp> metricRespList = getMetrics(metricReq.getModelId()).stream().filter(
                o -> o.getId().equals(metricReq.getId())).collect(Collectors.toList());
        updateMetric(metric);
        //动态更新字典
        String type = DictWordType.METRIC.getType();
        //MetricResp metricResp = getMetric(metric.getModelId(), metric.getBizName());
        if (!CollectionUtils.isEmpty(metricRespList)) {
            log.info("metricRespList size:{}", metricRespList.size());
            log.info("name:{}", metricRespList.get(0).getName());
            applicationEventPublisher.publishEvent(
                    new DataUpdateEvent(this, metricRespList.get(0).getName(),
                            metricReq.getName(),
                            metric.getModelId(),
                            metricRespList.get(0).getId(), type));
        }
    }

    public void saveMetric(Metric metric) {
        MetricDO metricDO = MetricConverter.convert2MetricDO(metric);
        log.info("[save metric] metricDO:{}", JSONObject.toJSONString(metricDO));
        metricRepository.createMetric(metricDO);
        metric.setId(metricDO.getId());
    }

    protected void updateMetric(Metric metric) {
        MetricDO metricDO = metricRepository.getMetricById(metric.getId());
        metricRepository.updateMetric(MetricConverter.convert(metricDO, metric));
    }

    public List<MetricResp> getMetricByModelId(Long modelId) {
        return convertList(getMetricDOByModelId(modelId));
    }

    protected List<MetricDO> getMetricDOByModelId(Long modelId) {
        List<MetricDO> metricDOS = metricRepository.getAllMetricList();
        return metricDOS.stream().filter(metricDO -> Objects.equals(metricDO.getModelId(), modelId))
                .collect(Collectors.toList());
    }

    @Override
    public List<MetricResp> getHighSensitiveMetric(Long modelId) {
        List<MetricResp> metricResps = getMetricByModelId(modelId);
        if (CollectionUtils.isEmpty(metricResps)) {
            return metricResps;
        }
        return metricResps.stream()
                .filter(metricResp -> SensitiveLevelEnum.HIGH.getCode().equals(metricResp.getSensitiveLevel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MetricResp> getAllHighSensitiveMetric() {
        List<MetricResp> metricResps = Lists.newArrayList();
        List<MetricDO> metricDOS = metricRepository.getAllMetricList();
        if (CollectionUtils.isEmpty(metricDOS)) {
            return metricResps;
        }
        return convertList(metricDOS.stream()
                .filter(metricResp -> SensitiveLevelEnum.HIGH.getCode().equals(metricResp.getSensitiveLevel()))
                .collect(Collectors.toList()));
    }

    @Override
    public void deleteMetric(Long id) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            throw new RuntimeException(String.format("the metric %s not exist", id));
        }
        metricRepository.deleteMetric(id);
        //动态更新字典
        String type = DictWordType.METRIC.getType();
        applicationEventPublisher.publishEvent(
                new DataDeleteEvent(this, metricDO.getName(), metricDO.getModelId(), metricDO.getId(), type));
    }

    @Override
    public List<String> mockAlias(MetricReq metricReq, String mockType, User user) {

        String mockAlias = chatGptHelper.mockAlias(mockType, metricReq.getName(), metricReq.getBizName(), "",
                metricReq.getDescription(), !"".equals(metricReq.getDataFormatType()));
        return JSONObject.parseObject(mockAlias, new TypeReference<List<String>>() {
        });
    }

    @Override
    public Set<String> getMetricTags() {
        List<MetricResp> metricResps = getMetrics();
        if (CollectionUtils.isEmpty(metricResps)) {
            return new HashSet<>();
        }
        return metricResps.stream().flatMap(metricResp ->
                metricResp.getTags().stream()).collect(Collectors.toSet());
    }

    @Override
    public List<DrillDownDimension> getDrillDownDimension(Long metricId) {
        MetricResp metricResp = getMetric(metricId);
        if (metricResp == null) {
            return Lists.newArrayList();
        }
        if (metricResp.getRelateDimension() != null
                && !CollectionUtils.isEmpty(metricResp.getRelateDimension().getDrillDownDimensions())) {
            return metricResp.getRelateDimension().getDrillDownDimensions();
        }
        ModelResp modelResp = modelService.getModel(metricResp.getModelId());
        return modelResp.getDrillDownDimensions();
    }

    private void saveMetricBatch(List<Metric> metrics, User user) {
        if (CollectionUtils.isEmpty(metrics)) {
            return;
        }
        List<MetricDO> metricDOS = metrics.stream().peek(metric -> metric.createdBy(user.getName()))
                .map(MetricConverter::convert2MetricDO).collect(Collectors.toList());
        log.info("[save metric] metrics:{}", JSONObject.toJSONString(metricDOS));
        metricRepository.createMetricBatch(metricDOS);
    }

    private void preCheckMetric(MetricReq metricReq) {
        MetricTypeParams typeParams = metricReq.getTypeParams();
        List<Measure> measures = typeParams.getMeasures();
        if (CollectionUtils.isEmpty(measures)) {
            throw new InvalidArgumentException("不可缺少度量");
        }
        if (StringUtils.isBlank(typeParams.getExpr())) {
            throw new InvalidArgumentException("表达式不可为空");
        }
        if (NameCheckUtils.containsSpecialCharacters(metricReq.getName())) {
            throw new InvalidArgumentException("名称包含特殊字符, 请修改");
        }
    }

    private void check(List<MetricReq> exprMetricReqList) {
        Long modelId = exprMetricReqList.get(0).getModelId();
        List<MetricResp> metricResps = getMetrics(modelId);
        for (MetricReq exprMetricReq : exprMetricReqList) {
            for (MetricResp metricResp : metricResps) {
                if (metricResp.getName().equalsIgnoreCase(exprMetricReq.getName())) {
                    throw new RuntimeException(String.format("存在相同的指标名:%s", metricResp.getName()));
                }
                if (metricResp.getBizName().equalsIgnoreCase(exprMetricReq.getBizName())) {
                    throw new RuntimeException(String.format("存在相同的指标名:%s", metricResp.getBizName()));
                }
                preCheckMetric(exprMetricReq);
            }
        }
    }

    private List<MetricResp> convertList(List<MetricDO> metricDOS) {
        List<MetricResp> metricResps = Lists.newArrayList();
        Map<Long, ModelResp> modelMap = modelService.getModelMap();
        if (!CollectionUtils.isEmpty(metricDOS)) {
            metricResps = metricDOS.stream()
                    .map(metricDO -> MetricConverter.convert2MetricResp(metricDO, modelMap))
                    .collect(Collectors.toList());
        }
        return metricResps;
    }


}
