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

    private DatasourceService datasourceService;

    private DomainService domainService;


    public DimensionServiceImpl(DimensionRepository dimensionRepository,
            DomainService domainService,
            DatasourceService datasourceService) {
        this.domainService = domainService;
        this.dimensionRepository = dimensionRepository;
        this.datasourceService = datasourceService;
    }

    @Override
    public void createDimension(DimensionReq dimensionReq, User user) {
        checkExist(Lists.newArrayList(dimensionReq));
        Dimension dimension = DimensionConverter.convert(dimensionReq);
        log.info("[create dimension] object:{}", JSONObject.toJSONString(dimension));
        dimension.createdBy(user.getName());
        saveDimension(dimension);
    }


    @Override
    public void createDimensionBatch(List<DimensionReq> dimensionReqs, User user) {
        if (CollectionUtils.isEmpty(dimensionReqs)) {
            return;
        }
        Long domainId = dimensionReqs.get(0).getDomainId();
        List<DimensionResp> dimensionResps = getDimensions(domainId);
        Map<String, DimensionResp> dimensionRespMap = dimensionResps.stream()
                .collect(Collectors.toMap(DimensionResp::getBizName, a -> a, (k1, k2) -> k1));
        List<Dimension> dimensions = dimensionReqs.stream().map(DimensionConverter::convert)
                .collect(Collectors.toList());
        List<Dimension> dimensionToInsert = dimensions.stream()
                .filter(dimension -> !dimensionRespMap.containsKey(dimension.getBizName()))
                .collect(Collectors.toList());

        log.info("[create dimension] object:{}", JSONObject.toJSONString(dimensions));
        saveDimensionBatch(dimensionToInsert, user);
    }

    @Override
    public void updateDimension(DimensionReq dimensionReq, User user) {
        Dimension dimension = DimensionConverter.convert(dimensionReq);
        dimension.updatedBy(user.getName());
        log.info("[update dimension] object:{}", JSONObject.toJSONString(dimension));
        updateDimension(dimension);
    }

    protected void updateDimension(Dimension dimension) {
        DimensionDO dimensionDO = dimensionRepository.getDimensionById(dimension.getId());
        dimensionRepository.updateDimension(DimensionConverter.convert(dimensionDO, dimension));
    }


    @Override
    public DimensionResp getDimension(String bizName, Long domainId) {
        List<DimensionResp> dimensionResps = getDimensions(domainId);
        if (CollectionUtils.isEmpty(dimensionResps)) {
            return null;
        }
        for (DimensionResp dimensionResp : dimensionResps) {
            if (dimensionResp.getBizName().equalsIgnoreCase(bizName)) {
                return dimensionResp;
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
        List<DimensionResp> dimensionResps = Lists.newArrayList();
        List<DimensionDO> dimensionDOS = dimensionRepository.getDimensionListByIds(ids);
        Map<Long, String> fullDomainPathMap = domainService.getDomainFullPath();
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensionResps = dimensionDOS.stream()
                    .map(dimensionDO -> DimensionConverter.convert2DimensionResp(dimensionDO, fullDomainPathMap,
                            new HashMap<>()))
                    .collect(Collectors.toList());
        }
        return dimensionResps;
    }

    @Override
    public List<DimensionResp> getDimensions(Long domainId) {
        return convertList(getDimensionDOS(domainId), datasourceService.getDatasourceMap());
    }

    @Override
    public List<DimensionResp> getDimensions() {
        return convertList(getDimensionDOS(), datasourceService.getDatasourceMap());
    }

    @Override
    public List<DimensionResp> getDimensionsByDatasource(Long datasourceId) {
        List<DimensionResp> dimensionResps = Lists.newArrayList();
        List<DimensionDO> dimensionDOS = dimensionRepository.getDimensionListOfDatasource(datasourceId);
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensionResps = dimensionDOS.stream()
                    .map(dimensionDO -> DimensionConverter.convert2DimensionResp(dimensionDO, new HashMap<>(),
                            new HashMap<>()))
                    .collect(Collectors.toList());
        }
        return dimensionResps;
    }

    private List<DimensionResp> convertList(List<DimensionDO> dimensionDOS,
            Map<Long, DatasourceResp> datasourceRespMap) {
        List<DimensionResp> dimensionResps = Lists.newArrayList();
        Map<Long, String> fullDomainPathMap = domainService.getDomainFullPath();
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensionResps = dimensionDOS.stream()
                    .map(dimensionDO -> DimensionConverter.convert2DimensionResp(dimensionDO, fullDomainPathMap,
                            datasourceRespMap))
                    .collect(Collectors.toList());
        }
        return dimensionResps;
    }


    @Override
    public List<DimensionResp> getHighSensitiveDimension(Long domainId) {
        List<DimensionResp> dimensionResps = getDimensions(domainId);
        if (CollectionUtils.isEmpty(dimensionResps)) {
            return dimensionResps;
        }
        return dimensionResps.stream()
                .filter(dimensionResp -> SensitiveLevelEnum.HIGH.getCode().equals(dimensionResp.getSensitiveLevel()))
                .collect(Collectors.toList());
    }


    protected List<DimensionDO> getDimensionDOS(Long domainId) {
        return dimensionRepository.getDimensionListOfDomain(domainId);
    }

    protected List<DimensionDO> getDimensionDOS() {
        return dimensionRepository.getDimensionList();
    }


    @Override
    public List<DimensionResp> getAllHighSensitiveDimension() {
        List<DimensionResp> dimensionResps = Lists.newArrayList();
        List<DimensionDO> dimensionDOS = dimensionRepository.getAllDimensionList();
        if (CollectionUtils.isEmpty(dimensionDOS)) {
            return dimensionResps;
        }
        return convertList(dimensionDOS.stream()
                .filter(dimensionDO -> SensitiveLevelEnum.HIGH.getCode().equals(dimensionDO.getSensitiveLevel()))
                .collect(Collectors.toList()), new HashMap<>());
    }


    public void saveDimension(Dimension dimension) {
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
    public void deleteDimension(Long id) {
        DimensionDO dimensionDO = dimensionRepository.getDimensionById(id);
        if (dimensionDO == null) {
            throw new RuntimeException(String.format("the dimension %s not exist", id));
        }
        dimensionRepository.deleteDimension(id);
    }


    private void checkExist(List<DimensionReq> dimensionReqs) {
        Long domainId = dimensionReqs.get(0).getDomainId();
        List<DimensionResp> dimensionResps = getDimensions(domainId);
        for (DimensionReq dimensionReq : dimensionReqs) {
            for (DimensionResp dimensionResp : dimensionResps) {
                if (dimensionResp.getName().equalsIgnoreCase(dimensionReq.getBizName())) {
                    throw new RuntimeException(String.format("exist same dimension name:%s", dimensionReq.getName()));
                }
                if (dimensionResp.getBizName().equalsIgnoreCase(dimensionReq.getBizName())) {
                    throw new RuntimeException(
                            String.format("exist same dimension bizName:%s", dimensionReq.getBizName()));
                }
            }
        }
    }

}
