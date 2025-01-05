package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.DataSetDetail;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.request.*;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSetDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DataSetDOMapper;
import com.tencent.supersonic.headless.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataSetServiceImpl extends ServiceImpl<DataSetDOMapper, DataSetDO>
        implements DataSetService {

    @Autowired
    private DomainService domainService;

    @Lazy
    @Autowired
    private DimensionService dimensionService;

    @Lazy
    @Autowired
    private MetricService metricService;

    @Override
    public DataSetResp save(DataSetReq dataSetReq, User user) {
        dataSetReq.createdBy(user.getName());
        DataSetDO dataSetDO = convert(dataSetReq);
        dataSetDO.setStatus(StatusEnum.ONLINE.getCode());
        DataSetResp dataSetResp = convert(dataSetDO);
        // conflictCheck(dataSetResp);
        save(dataSetDO);
        dataSetResp.setId(dataSetDO.getId());
        return dataSetResp;
    }

    @Override
    public DataSetResp update(DataSetReq dataSetReq, User user) {
        dataSetReq.updatedBy(user.getName());
        DataSetDO dataSetDO = convert(dataSetReq);
        DataSetResp dataSetResp = convert(dataSetDO);
        // conflictCheck(dataSetResp);
        updateById(dataSetDO);
        return dataSetResp;
    }

    @Override
    public DataSetResp getDataSet(Long id) {
        DataSetDO dataSetDO = getById(id);
        return convert(dataSetDO);
    }

    @Override
    public List<DataSetResp> getDataSetList(MetaFilter metaFilter) {
        QueryWrapper<DataSetDO> wrapper = new QueryWrapper<>();
        if (metaFilter.getDomainId() != null) {
            wrapper.lambda().eq(DataSetDO::getDomainId, metaFilter.getDomainId());
        }
        if (!CollectionUtils.isEmpty(metaFilter.getIds())) {
            wrapper.lambda().in(DataSetDO::getId, metaFilter.getIds());
        }
        if (metaFilter.getStatus() != null) {
            wrapper.lambda().eq(DataSetDO::getStatus, metaFilter.getStatus());
        }
        if (metaFilter.getName() != null) {
            wrapper.lambda().eq(DataSetDO::getName, metaFilter.getName());
        }
        if (!CollectionUtils.isEmpty(metaFilter.getNames())) {
            wrapper.lambda().in(DataSetDO::getName, metaFilter.getNames());
        }
        wrapper.lambda().ne(DataSetDO::getStatus, StatusEnum.DELETED.getCode());
        return list(wrapper).stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id, User user) {
        DataSetDO dataSetDO = getById(id);
        dataSetDO.setStatus(StatusEnum.DELETED.getCode());
        dataSetDO.setUpdatedBy(user.getName());
        dataSetDO.setUpdatedAt(new Date());
        updateById(dataSetDO);
    }

    @Override
    public List<DataSetResp> getDataSets(String dataSetName, User user) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setName(dataSetName);
        return getDataSetsByAuth(user, metaFilter);
    }

    @Override
    public List<DataSetResp> getDataSets(List<String> dataSetNames, User user) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setNames(dataSetNames);
        return getDataSetsByAuth(user, metaFilter);
    }

    private List<DataSetResp> getDataSetsByAuth(User user, MetaFilter metaFilter) {
        List<DataSetResp> dataSetResps = getDataSetList(metaFilter);
        return getDataSetFilterByAuth(dataSetResps, user);
    }

    @Override
    public List<DataSetResp> getDataSetsInheritAuth(User user, Long domainId) {
        List<DataSetResp> dataSetResps = getDataSetList(new MetaFilter());
        List<DataSetResp> inheritAuthFormDomain = getDataSetFilterByDomainAuth(dataSetResps, user);
        Set<DataSetResp> dataSetRespSet = new HashSet<>(inheritAuthFormDomain);
        List<DataSetResp> dataSetFilterByAuth = getDataSetFilterByAuth(dataSetResps, user);
        dataSetRespSet.addAll(dataSetFilterByAuth);
        if (domainId != null && domainId > 0) {
            dataSetRespSet = dataSetRespSet.stream()
                    .filter(modelResp -> modelResp.getDomainId().equals(domainId))
                    .collect(Collectors.toSet());
        }
        return dataSetRespSet.stream().sorted(Comparator.comparingLong(DataSetResp::getId))
                .collect(Collectors.toList());
    }

    private List<DataSetResp> getDataSetFilterByAuth(List<DataSetResp> dataSetResps, User user) {
        return dataSetResps.stream().filter(dataSetResp -> checkAdminPermission(user, dataSetResp))
                .collect(Collectors.toList());
    }

    private List<DataSetResp> getDataSetFilterByDomainAuth(List<DataSetResp> dataSetResps,
            User user) {
        Set<DomainResp> domainResps = domainService.getDomainAuthSet(user, AuthType.ADMIN);
        if (CollectionUtils.isEmpty(domainResps)) {
            return Lists.newArrayList();
        }
        Set<Long> domainIds =
                domainResps.stream().map(DomainResp::getId).collect(Collectors.toSet());
        return dataSetResps.stream()
                .filter(dataSetResp -> domainIds.contains(dataSetResp.getDomainId()))
                .collect(Collectors.toList());
    }

    private DataSetResp convert(DataSetDO dataSetDO) {
        DataSetResp dataSetResp = new DataSetResp();
        BeanMapper.mapper(dataSetDO, dataSetResp);
        dataSetResp.setDataSetDetail(
                JSONObject.parseObject(dataSetDO.getDataSetDetail(), DataSetDetail.class));
        if (dataSetDO.getQueryConfig() != null) {
            dataSetResp.setQueryConfig(
                    JSONObject.parseObject(dataSetDO.getQueryConfig(), QueryConfig.class));
        }
        dataSetResp.setAdmins(StringUtils.isBlank(dataSetDO.getAdmin()) ? Lists.newArrayList()
                : Arrays.asList(dataSetDO.getAdmin().split(",")));
        dataSetResp.setAdminOrgs(StringUtils.isBlank(dataSetDO.getAdminOrg()) ? Lists.newArrayList()
                : Arrays.asList(dataSetDO.getAdminOrg().split(",")));
        dataSetResp.setTypeEnum(TypeEnums.DATASET);

        return dataSetResp;
    }

    private DataSetDO convert(DataSetReq dataSetReq) {
        DataSetDO dataSetDO = new DataSetDO();
        BeanMapper.mapper(dataSetReq, dataSetDO);
        dataSetDO.setDataSetDetail(JSONObject.toJSONString(dataSetReq.getDataSetDetail()));
        dataSetDO.setQueryConfig(JSONObject.toJSONString(dataSetReq.getQueryConfig()));
        return dataSetDO;
    }

    public SemanticQueryReq convert(QueryDataSetReq queryDataSetReq) {
        SemanticQueryReq queryReq = new QueryStructReq();
        if (StringUtils.isNotBlank(queryDataSetReq.getSql())) {
            queryReq = new QuerySqlReq();
        }
        BeanUtils.copyProperties(queryDataSetReq, queryReq);
        if (Objects.nonNull(queryDataSetReq.getQueryType())
                && QueryType.DETAIL.equals(queryDataSetReq.getQueryType())) {
            queryReq.setInnerLayerNative(true);
        }
        return queryReq;
    }

    public static boolean checkAdminPermission(User user, DataSetResp dataSetResp) {
        List<String> admins = dataSetResp.getAdmins();
        if (user.isSuperAdmin()) {
            return true;
        }
        String userName = user.getName();
        return admins.contains(userName) || dataSetResp.getCreatedBy().equals(userName);
    }

    @Override
    public Map<Long, List<Long>> getModelIdToDataSetIds(List<Long> dataSetIds, User user) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setStatus(StatusEnum.ONLINE.getCode());
        metaFilter.setIds(dataSetIds);
        List<DataSetResp> dataSetList = getDataSetList(metaFilter);
        return dataSetList.stream()
                .flatMap(dataSetResp -> dataSetResp.getAllModels().stream()
                        .map(modelId -> Pair.of(modelId, dataSetResp.getId())))
                .collect(Collectors.groupingBy(Pair::getLeft,
                        Collectors.mapping(Pair::getRight, Collectors.toList())));
    }

    @Override
    public Map<Long, List<Long>> getModelIdToDataSetIds() {
        return getModelIdToDataSetIds(Lists.newArrayList(), User.getDefaultUser());
    }

    private void conflictCheck(DataSetResp dataSetResp) {
        List<Long> allDimensionIds = dataSetResp.dimensionIds();
        List<Long> allMetricIds = dataSetResp.metricIds();
        MetaFilter metaFilter = new MetaFilter();
        if (!CollectionUtils.isEmpty(allDimensionIds)) {
            metaFilter.setIds(allDimensionIds);
            List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
            List<String> duplicateDimensionNames =
                    findDuplicates(dimensionResps, DimensionResp::getName);
            if (!duplicateDimensionNames.isEmpty()) {
                throw new InvalidArgumentException("存在相同的维度名: " + duplicateDimensionNames);
            }
        }
        if (!CollectionUtils.isEmpty(allMetricIds)) {
            metaFilter.setIds(allMetricIds);
            List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
            List<String> duplicateMetricNames = findDuplicates(metricResps, MetricResp::getName);
            if (!duplicateMetricNames.isEmpty()) {
                throw new InvalidArgumentException("存在相同的指标名: " + duplicateMetricNames);
            }
        }
    }

    private <T, R> List<String> findDuplicates(List<T> list, Function<T, R> keyExtractor) {
        return list.stream().collect(Collectors.groupingBy(keyExtractor, Collectors.counting()))
                .entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey)
                .map(Object::toString).collect(Collectors.toList());
    }

    public Long getDataSetIdFromSql(String sql, User user) {
        List<DataSetResp> dataSets = null;
        try {
            String tableName = SqlSelectHelper.getTableName(sql);
            dataSets = getDataSets(tableName, user);
        } catch (Exception e) {
            log.error("getDataSetIdFromSql error:{}", e);
        }
        if (org.apache.commons.collections.CollectionUtils.isEmpty(dataSets)) {
            throw new InvalidArgumentException("从Sql参数中无法获取到DataSetId");
        }
        Long dataSetId = dataSets.get(0).getId();
        log.info("getDataSetIdFromSql dataSetId:{}", dataSetId);
        return dataSetId;
    }
}
