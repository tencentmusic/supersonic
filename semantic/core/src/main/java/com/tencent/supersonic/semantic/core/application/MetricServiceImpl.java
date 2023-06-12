package com.tencent.supersonic.semantic.core.application;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.pojo.Measure;
import com.tencent.supersonic.semantic.api.core.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.core.request.MetricReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.DomainResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.common.enums.SensitiveLevelEnum;
import com.tencent.supersonic.semantic.core.domain.dataobject.MetricDO;
import com.tencent.supersonic.semantic.core.domain.manager.MetricYamlManager;
import com.tencent.supersonic.semantic.core.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.core.domain.repository.MetricRepository;
import com.tencent.supersonic.semantic.core.domain.utils.MetricConverter;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.core.domain.MetricService;
import com.tencent.supersonic.semantic.core.domain.pojo.Metric;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class MetricServiceImpl implements MetricService {


    private MetricRepository metricRepository;

    private MetricYamlManager metricYamlManager;

    private DomainService domainService;


    public MetricServiceImpl(MetricRepository metricRepository,
            MetricYamlManager metricYamlManager,
            DomainService domainService) {
        this.domainService = domainService;
        this.metricRepository = metricRepository;
        this.metricYamlManager = metricYamlManager;
    }

    @Override
    public void creatExprMetric(MetricReq metricReq, User user) throws Exception {
        checkExist(Lists.newArrayList(metricReq));
        Metric metric = MetricConverter.convert(metricReq);
        metric.createdBy(user.getName());
        log.info("[create metric] object:{}", JSONObject.toJSONString(metric));
        saveMetricAndGenerateYaml(metric);
    }

    @Override
    public void createMetricBatch(List<MetricReq> metricReqs, User user) throws Exception {
        if (CollectionUtils.isEmpty(metricReqs)) {
            return;
        }
        List<Metric> metrics = metricReqs.stream().map(MetricConverter::convert).collect(Collectors.toList());
        Long domainId = metricReqs.get(0).getDomainId();
        List<MetricResp> metricDescs = getMetricByDomainId(domainId);
        Map<String, MetricResp> metricDescMap = metricDescs.stream()
                .collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
        List<Metric> metricToInsert = metrics.stream()
                .filter(metric -> !metricDescMap.containsKey(metric.getBizName())).collect(Collectors.toList());
        log.info("[insert metric] object:{}", JSONObject.toJSONString(metricToInsert));
        saveMetricBatch(metricToInsert, user);

        generateYamlFile(metrics.get(0).getDomainId());
    }


    @Override
    public List<MetricResp> getMetrics(Long domainId) {
        return convertList(metricRepository.getMetricList(domainId));
    }

    @Override
    public List<MetricResp> getMetrics(Long domainId, Long datasourceId) {
        List<MetricResp> metricResps = convertList(metricRepository.getMetricList(domainId));
        return metricResps.stream().filter(metricResp -> {
            Set<Long> datasourceIdSet = metricResp.getTypeParams().getMeasures().stream()
                    .map(Measure::getDatasourceId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            return !CollectionUtils.isEmpty(datasourceIdSet) && datasourceIdSet.contains(datasourceId);
        }).collect(Collectors.toList());
    }

    public List<MetricResp> getMetrics(List<Long> ids) {
        return convertList(metricRepository.getMetricListByIds(ids));
    }

    @Override
    public PageInfo<MetricResp> queryMetric(PageMetricReq pageMetrricReq) {
        if (pageMetrricReq.getDomainId() == null) {
            return PageInfo.of(Lists.newArrayList());
        }
        MetricFilter metricFilter = new MetricFilter();
        BeanUtils.copyProperties(pageMetrricReq, metricFilter);
        PageInfo<MetricDO> metricDOPageInfo = PageHelper.startPage(pageMetrricReq.getCurrent(),
                        pageMetrricReq.getPageSize())
                .doSelectPageInfo(() -> queryMetric(metricFilter));
        PageInfo<MetricResp> pageInfo = new PageInfo<>();
        BeanUtils.copyProperties(metricDOPageInfo, pageInfo);
        pageInfo.setList(convertList(metricDOPageInfo.getList()));
        return pageInfo;
    }

    private List<MetricDO> queryMetric(MetricFilter metricFilter) {
        return metricRepository.getMetric(metricFilter);
    }


    @Override
    public MetricResp getMetric(Long domainId, String bizName) {
        List<MetricResp> metricDescs = getMetricByDomainId(domainId);
        MetricResp metricDesc = null;
        if (CollectionUtils.isEmpty(metricDescs)) {
            return metricDesc;
        }
        for (MetricResp metric : metricDescs) {
            if (metric.getBizName().equalsIgnoreCase(bizName)) {
                metricDesc = metric;
            }
        }
        return metricDesc;
    }

    @Override
    public void updateExprMetric(MetricReq metricReq, User user) throws Exception {
        preCheckMetric(metricReq);
        Metric metric = MetricConverter.convert(metricReq);
        metric.updatedBy(user.getName());
        log.info("[update metric] object:{}", JSONObject.toJSONString(metric));
        updateMetric(metric);
        generateYamlFile(metric.getDomainId());
    }


    public List<Metric> getMetricList(Long domainId) {
        List<Metric> metrics = Lists.newArrayList();
        List<MetricDO> metricDOS = metricRepository.getMetricList(domainId);
        if (!CollectionUtils.isEmpty(metricDOS)) {
            metrics = metricDOS.stream().map(MetricConverter::convert2Metric).collect(Collectors.toList());
        }
        return metrics;
    }


    //保存并获取自增ID
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


    public List<MetricResp> getMetricByDomainId(Long domainId) {
        return convertList(getMetricDOByDomainId(domainId));
    }

    protected List<MetricDO> getMetricDOByDomainId(Long domainId) {
        List<MetricDO> metricDOS = metricRepository.getAllMetricList();
        return metricDOS.stream().filter(metricDO -> Objects.equals(metricDO.getDomainId(), domainId))
                .collect(Collectors.toList());
    }


    @Override
    public List<MetricResp> getHighSensitiveMetric(Long domainId) {
        List<MetricResp> metricDescs = getMetricByDomainId(domainId);
        if (CollectionUtils.isEmpty(metricDescs)) {
            return metricDescs;
        }
        return metricDescs.stream()
                .filter(metricDesc -> SensitiveLevelEnum.HIGH.getCode().equals(metricDesc.getSensitiveLevel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MetricResp> getAllHighSensitiveMetric() {
        List<MetricResp> metricDescs = Lists.newArrayList();
        List<MetricDO> metricDOS = metricRepository.getAllMetricList();
        if (CollectionUtils.isEmpty(metricDOS)) {
            return metricDescs;
        }
        return convertList(metricDOS.stream()
                .filter(metricDesc -> SensitiveLevelEnum.HIGH.getCode().equals(metricDesc.getSensitiveLevel()))
                .collect(Collectors.toList()));
    }

    @Override
    public void deleteMetric(Long id) throws Exception {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            throw new RuntimeException(String.format("the metric %s not exist", id));
        }
        metricRepository.deleteMetric(id);
        generateYamlFile(metricDO.getDomainId());
    }

    protected void generateYamlFile(Long domainId) throws Exception {
        List<Metric> metrics = getMetricList(domainId);
        String fullPath = domainService.getDomainFullPath(domainId);
        String domainBizName = domainService.getDomainBizName(domainId);
        metricYamlManager.generateYamlFile(metrics, fullPath, domainBizName);
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

    private void saveMetricAndGenerateYaml(Metric metric) throws Exception {
        saveMetric(metric);
        generateYamlFile(metric.getDomainId());
    }


    private void preCheckMetric(MetricReq exprMetricReq) {

        MetricTypeParams typeParams = exprMetricReq.getTypeParams();
        List<Measure> measures = typeParams.getMeasures();
        if (CollectionUtils.isEmpty(measures)) {
            throw new RuntimeException("measure can not be none");
        }
        for (Measure measure : measures) {
            measure.setExpr(null);
        }
    }

    private void checkExist(List<MetricReq> exprMetricReqList) {
        Long domainId = exprMetricReqList.get(0).getDomainId();
        List<MetricResp> metricDescs = getMetrics(domainId);
        for (MetricReq exprMetricReq : exprMetricReqList) {
            for (MetricResp metricDesc : metricDescs) {
                if (metricDesc.getName().equalsIgnoreCase(exprMetricReq.getName())) {
                    throw new RuntimeException(String.format("exist same metric name:%s", metricDesc.getName()));
                }
                if (metricDesc.getBizName().equalsIgnoreCase(exprMetricReq.getBizName())) {
                    throw new RuntimeException(String.format("exist same metric en name:%s", metricDesc.getName()));
                }
                preCheckMetric(exprMetricReq);
            }
        }
    }


    private List<MetricResp> convertList(List<MetricDO> metricDOS) {
        List<MetricResp> metricDescs = Lists.newArrayList();
        Map<Long, DomainResp> domainMap = domainService.getDomainMap();
        if (!CollectionUtils.isEmpty(metricDOS)) {
            metricDescs = metricDOS.stream()
                    .map(metricDO -> MetricConverter.convert2MetricDesc(metricDO, domainMap))
                    .collect(Collectors.toList());
        }
        return metricDescs;
    }


}
