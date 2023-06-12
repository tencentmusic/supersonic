package com.tencent.supersonic.semantic.core.application;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.DimensionReq;
import com.tencent.supersonic.semantic.api.core.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.core.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.common.enums.SensitiveLevelEnum;
import com.tencent.supersonic.semantic.core.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.core.domain.manager.DimensionYamlManager;
import com.tencent.supersonic.semantic.core.domain.repository.DimensionRepository;
import com.tencent.supersonic.semantic.core.domain.utils.DimensionConverter;
import com.tencent.supersonic.semantic.core.domain.DatasourceService;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.core.domain.pojo.Dimension;
import com.tencent.supersonic.semantic.core.domain.pojo.DimensionFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service
@Slf4j
public class DimensionServiceImpl implements DimensionService {


    private DimensionRepository dimensionRepository;

    private DimensionYamlManager dimensionYamlManager;

    private DatasourceService datasourceService;

    private DomainService domainService;


    public DimensionServiceImpl(DimensionRepository dimensionRepository,
            DimensionYamlManager dimensionYamlManager,
            DomainService domainService,
            DatasourceService datasourceService) {
        this.domainService = domainService;
        this.dimensionRepository = dimensionRepository;
        this.dimensionYamlManager = dimensionYamlManager;
        this.datasourceService = datasourceService;
    }

    @Override
    public void createDimension(DimensionReq dimensionReq, User user) throws Exception {
        checkExist(Lists.newArrayList(dimensionReq));
        Dimension dimension = DimensionConverter.convert(dimensionReq);
        log.info("[create dimension] object:{}", JSONObject.toJSONString(dimension));
        saveDimensionAndGenerateYaml(dimension, user);
    }


    @Override
    public void createDimensionBatch(List<DimensionReq> dimensionReqs, User user) throws Exception {
        if (CollectionUtils.isEmpty(dimensionReqs)) {
            return;
        }
        Long domainId = dimensionReqs.get(0).getDomainId();
        List<DimensionResp> dimensionDescs = getDimensions(domainId);
        Map<String, DimensionResp> dimensionDescMap = dimensionDescs.stream()
                .collect(Collectors.toMap(DimensionResp::getBizName, a -> a, (k1, k2) -> k1));
        List<Dimension> dimensions = dimensionReqs.stream().map(DimensionConverter::convert)
                .collect(Collectors.toList());
        List<Dimension> dimensionToInsert = dimensions.stream()
                .filter(dimension -> !dimensionDescMap.containsKey(dimension.getBizName()))
                .collect(Collectors.toList());

        log.info("[create dimension] object:{}", JSONObject.toJSONString(dimensions));
        saveDimensionBatch(dimensionToInsert, user);
        generateYamlFile(dimensions.get(0).getDatasourceId(), dimensions.get(0).getDomainId());
    }

    @Override
    public void updateDimension(DimensionReq dimensionReq, User user) throws Exception {
        Dimension dimension = DimensionConverter.convert(dimensionReq);
        dimension.updatedBy(user.getName());
        log.info("[update dimension] object:{}", JSONObject.toJSONString(dimension));
        updateDimension(dimension);
        generateYamlFile(dimension.getDatasourceId(), dimension.getDomainId());
    }

    protected void updateDimension(Dimension dimension) {
        DimensionDO dimensionDO = dimensionRepository.getDimensionById(dimension.getId());
        dimensionRepository.updateDimension(DimensionConverter.convert(dimensionDO, dimension));
    }


    @Override
    public DimensionResp getDimension(String bizName, Long domainId) {
        List<DimensionResp> dimensionDescs = getDimensions(domainId);
        if (CollectionUtils.isEmpty(dimensionDescs)) {
            return null;
        }
        for (DimensionResp dimensionDesc : dimensionDescs) {
            if (dimensionDesc.getBizName().equalsIgnoreCase(bizName)) {
                return dimensionDesc;
            }
        }
        return null;
    }

    @Override
    public PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq) {
        DimensionFilter dimensionFilter = new DimensionFilter();
        BeanUtils.copyProperties(pageDimensionReq, dimensionFilter);
        PageInfo<DimensionDO> dimensionDOPageInfo = PageHelper.startPage(pageDimensionReq.getCurrent(),
                        pageDimensionReq.getPageSize())
                .doSelectPageInfo(() -> queryDimension(dimensionFilter));
        PageInfo<DimensionResp> pageInfo = new PageInfo<>();
        BeanUtils.copyProperties(dimensionDOPageInfo, pageInfo);
        pageInfo.setList(convertList(dimensionDOPageInfo.getList(), datasourceService.getDatasourceMap()));
        return pageInfo;
    }

    private List<DimensionDO> queryDimension(DimensionFilter dimensionFilter) {
        return dimensionRepository.getDimension(dimensionFilter);
    }


    @Override
    public List<DimensionResp> getDimensions(List<Long> ids) {
        List<DimensionResp> dimensionDescs = Lists.newArrayList();
        List<DimensionDO> dimensionDOS = dimensionRepository.getDimensionListByIds(ids);
        Map<Long, String> fullDomainPathMap = domainService.getDomainFullPath();
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensionDescs = dimensionDOS.stream()
                    .map(dimensionDO -> DimensionConverter.convert2DimensionDesc(dimensionDO, fullDomainPathMap,
                            new HashMap<>()))
                    .collect(Collectors.toList());
        }
        return dimensionDescs;
    }

    @Override
    public List<DimensionResp> getDimensions(Long domainId) {
        return convertList(getDimensionDOS(domainId), datasourceService.getDatasourceMap());
    }


    public List<Dimension> getDimensionList(Long datasourceId) {
        List<Dimension> dimensions = Lists.newArrayList();
        List<DimensionDO> dimensionDOS = dimensionRepository.getDimensionListOfDatasource(datasourceId);
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensions = dimensionDOS.stream().map(DimensionConverter::convert2Dimension).collect(Collectors.toList());
        }
        return dimensions;
    }

    @Override
    public List<DimensionResp> getDimensionsByDatasource(Long datasourceId) {
        List<DimensionResp> dimensionDescs = Lists.newArrayList();
        List<DimensionDO> dimensionDOS = dimensionRepository.getDimensionListOfDatasource(datasourceId);
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensionDescs = dimensionDOS.stream()
                    .map(dimensionDO -> DimensionConverter.convert2DimensionDesc(dimensionDO, new HashMap<>(),
                            new HashMap<>()))
                    .collect(Collectors.toList());
        }
        return dimensionDescs;
    }

    private List<DimensionResp> convertList(List<DimensionDO> dimensionDOS,
            Map<Long, DatasourceResp> datasourceDescMap) {
        List<DimensionResp> dimensionDescs = Lists.newArrayList();
        Map<Long, String> fullDomainPathMap = domainService.getDomainFullPath();
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensionDescs = dimensionDOS.stream()
                    .map(dimensionDO -> DimensionConverter.convert2DimensionDesc(dimensionDO, fullDomainPathMap,
                            datasourceDescMap))
                    .collect(Collectors.toList());
        }
        return dimensionDescs;
    }



    @Override
    public List<DimensionResp> getHighSensitiveDimension(Long domainId) {
        List<DimensionResp> dimensionDescs = getDimensions(domainId);
        if (CollectionUtils.isEmpty(dimensionDescs)) {
            return dimensionDescs;
        }
        return dimensionDescs.stream()
                .filter(dimensionDesc -> SensitiveLevelEnum.HIGH.getCode().equals(dimensionDesc.getSensitiveLevel()))
                .collect(Collectors.toList());
    }


    protected List<DimensionDO> getDimensionDOS(Long domainId) {
        return dimensionRepository.getDimensionListOfDomain(domainId);
    }


    @Override
    public List<DimensionResp> getAllHighSensitiveDimension() {
        List<DimensionResp> dimensionDescs = Lists.newArrayList();
        List<DimensionDO> dimensionDOS = dimensionRepository.getAllDimensionList();
        if (CollectionUtils.isEmpty(dimensionDOS)) {
            return dimensionDescs;
        }
        return convertList(dimensionDOS.stream()
                .filter(dimensionDO -> SensitiveLevelEnum.HIGH.getCode().equals(dimensionDO.getSensitiveLevel()))
                .collect(Collectors.toList()), new HashMap<>());
    }


    //保存并获取自增ID
    private void saveDimension(Dimension dimension) {
        DimensionDO dimensionDO = DimensionConverter.convert2DimensionDO(dimension);
        log.info("[save dimension] dimensionDO:{}", JSONObject.toJSONString(dimensionDO));
        dimensionRepository.createDimension(dimensionDO);
        dimension.setId(dimensionDO.getId());
    }

    private void saveDimensionBatch(List<Dimension> dimensions, User user) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        dimensions = dimensions.stream().peek(dimension -> dimension.createdBy(user.getName()))
                .collect(Collectors.toList());
        List<DimensionDO> dimensionDOS = dimensions.stream()
                .map(DimensionConverter::convert2DimensionDO).collect(Collectors.toList());
        log.info("[save dimension] dimensionDO:{}", JSONObject.toJSONString(dimensionDOS));
        dimensionRepository.createDimensionBatch(dimensionDOS);
    }




    @Override
    public void deleteDimension(Long id) throws Exception {
        DimensionDO dimensionDO = dimensionRepository.getDimensionById(id);
        if (dimensionDO == null) {
            throw new RuntimeException(String.format("the dimension %s not exist", id));
        }
        dimensionRepository.deleteDimension(id);
        generateYamlFile(dimensionDO.getDatasourceId(), dimensionDO.getDomainId());
    }

    protected void generateYamlFile(Long datasourceId, Long domainId) throws Exception {
        String datasourceBizName = datasourceService.getSourceBizNameById(datasourceId);
        List<Dimension> dimensionList = getDimensionList(datasourceId);
        String fullPath = domainService.getDomainFullPath(domainId);
        dimensionYamlManager.generateYamlFile(dimensionList, fullPath, datasourceBizName);
    }


    private void checkExist(List<DimensionReq> dimensionReqs) {
        Long domainId = dimensionReqs.get(0).getDomainId();
        List<DimensionResp> dimensionDescs = getDimensions(domainId);
        for (DimensionReq dimensionReq : dimensionReqs) {
            for (DimensionResp dimensionDesc : dimensionDescs) {
                if (dimensionDesc.getName().equalsIgnoreCase(dimensionReq.getBizName())) {
                    throw new RuntimeException(String.format("exist same dimension name:%s", dimensionReq.getName()));
                }
                if (dimensionDesc.getBizName().equalsIgnoreCase(dimensionReq.getBizName())) {
                    throw new RuntimeException(
                            String.format("exist same dimension bizName:%s", dimensionReq.getBizName()));
                }
            }
        }

    }


    private void saveDimensionAndGenerateYaml(Dimension dimension, User user) throws Exception {
        dimension.createdBy(user.getName());
        saveDimension(dimension);
        generateYamlFile(dimension.getDatasourceId(), dimension.getDomainId());
    }





}
