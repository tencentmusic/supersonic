package com.tencent.supersonic.semantic.model.application;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.semantic.model.domain.dataobject.MetricDO;
import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.model.domain.repository.MetricRepository;
import com.tencent.supersonic.semantic.model.domain.utils.MetricConverter;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.pojo.Metric;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class MetricServiceImpl implements MetricService {


    private MetricRepository metricRepository;

    private DomainService domainService;


    public MetricServiceImpl(MetricRepository metricRepository,
            DomainService domainService) {
        this.domainService = domainService;
        this.metricRepository = metricRepository;
    }

    @Override
    public void creatExprMetric(MetricReq metricReq, User user) {
        checkExist(Lists.newArrayList(metricReq));
        Metric metric = MetricConverter.convert(metricReq);
        metric.createdBy(user.getName());
        log.info("[create metric] object:{}", JSONObject.toJSONString(metric));
        saveMetric(metric);
    }

    @Override
    public void createMetricBatch(List<MetricReq> metricReqs, User user) {
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
    }


    @Override
    public List<MetricResp> getMetrics(Long domainId) {
        return convertList(metricRepository.getMetricList(domainId));
    }

    @Override
    public List<MetricResp> getMetrics() {
        return convertList(metricRepository.getMetricList());
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
    public PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq) {
        MetricFilter metricFilter = new MetricFilter();
        BeanUtils.copyProperties(pageMetricReq, metricFilter);
        Set<DomainResp> domainResps = domainService.getDomainChildren(pageMetricReq.getDomainIds());
        List<Long> domainIds = domainResps.stream().map(DomainResp::getId).collect(Collectors.toList());
        metricFilter.setDomainIds(domainIds);
        PageInfo<MetricDO> metricDOPageInfo = PageHelper.startPage(pageMetricReq.getCurrent(),
                        pageMetricReq.getPageSize())
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
    public void updateExprMetric(MetricReq metricReq, User user) {
        preCheckMetric(metricReq);
        Metric metric = MetricConverter.convert(metricReq);
        metric.updatedBy(user.getName());
        log.info("[update metric] object:{}", JSONObject.toJSONString(metric));
        updateMetric(metric);
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
    public void deleteMetric(Long id) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            throw new RuntimeException(String.format("the metric %s not exist", id));
        }
        metricRepository.deleteMetric(id);
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
            throw new RuntimeException("measure can not be none");
        }
        if (StringUtils.isBlank(typeParams.getExpr())) {
            throw new RuntimeException("expr can not be blank");
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
