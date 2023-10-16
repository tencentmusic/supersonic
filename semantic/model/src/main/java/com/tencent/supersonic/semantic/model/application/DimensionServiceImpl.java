package com.tencent.supersonic.semantic.model.application;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DataAddEvent;
import com.tencent.supersonic.common.pojo.DataDeleteEvent;
import com.tencent.supersonic.common.pojo.DataUpdateEvent;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.ChatGptHelper;
import com.tencent.supersonic.semantic.api.model.pojo.DatasourceDetail;
import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.model.domain.repository.DimensionRepository;
import com.tencent.supersonic.semantic.model.domain.utils.DimensionConverter;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.pojo.Dimension;
import com.tencent.supersonic.semantic.model.domain.pojo.DimensionFilter;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.tencent.supersonic.semantic.model.domain.utils.NameCheckUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service
@Slf4j
public class DimensionServiceImpl implements DimensionService {


    private DimensionRepository dimensionRepository;

    private DatasourceService datasourceService;

    private ModelService modelService;

    private ChatGptHelper chatGptHelper;

    private DatabaseService databaseService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    public DimensionServiceImpl(DimensionRepository dimensionRepository,
                                ModelService modelService,
                                DatasourceService datasourceService,
                                ChatGptHelper chatGptHelper,
                                DatabaseService databaseService) {
        this.modelService = modelService;
        this.dimensionRepository = dimensionRepository;
        this.datasourceService = datasourceService;
        this.chatGptHelper = chatGptHelper;
        this.databaseService = databaseService;

    }

    @Override
    public void createDimension(DimensionReq dimensionReq, User user) {
        checkExist(Lists.newArrayList(dimensionReq));
        Dimension dimension = DimensionConverter.convert(dimensionReq);
        log.info("[create dimension] object:{}", JSONObject.toJSONString(dimension));
        dimension.createdBy(user.getName());
        saveDimension(dimension);
        String type = DictWordType.DIMENSION.getType();
        DimensionResp dimensionResp = getDimension(dimension.getBizName(), dimension.getModelId());
        applicationEventPublisher.publishEvent(
                new DataAddEvent(this, dimension.getName(), dimension.getModelId(), dimensionResp.getId(), type));
    }


    @Override
    public void createDimensionBatch(List<DimensionReq> dimensionReqs, User user) {
        if (CollectionUtils.isEmpty(dimensionReqs)) {
            return;
        }
        Long modelId = dimensionReqs.get(0).getModelId();
        List<DimensionResp> dimensionResps = getDimensions(modelId);
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
        if (NameCheckUtils.containsSpecialCharacters(dimensionReq.getName())) {
            throw new InvalidArgumentException("名称包含特殊字符, 请修改");
        }
        Dimension dimension = DimensionConverter.convert(dimensionReq);
        dimension.updatedBy(user.getName());
        log.info("[update dimension] object:{}", JSONObject.toJSONString(dimension));
        updateDimension(dimension);
        DimensionResp dimensionResp = getDimension(dimensionReq.getId());
        //动态更新字典
        String type = DictWordType.DIMENSION.getType();
        if (dimensionResp != null) {
            applicationEventPublisher.publishEvent(
                    new DataUpdateEvent(this, dimensionResp.getName(),
                            dimensionReq.getName(),
                            dimension.getModelId(),
                            dimensionResp.getId(), type));
        }
    }

    protected void updateDimension(Dimension dimension) {
        DimensionDO dimensionDO = dimensionRepository.getDimensionById(dimension.getId());
        dimensionRepository.updateDimension(DimensionConverter.convert(dimensionDO, dimension));
    }


    @Override
    public DimensionResp getDimension(String bizName, Long modelId) {
        List<DimensionResp> dimensionResps = getDimensions(modelId);
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
    public DimensionResp getDimension(Long id) {
        DimensionDO dimensionDO = dimensionRepository.getDimensionById(id);
        return DimensionConverter.convert2DimensionResp(dimensionDO, new HashMap<>(), new HashMap<>());
    }

    @Override
    public PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq) {
        DimensionFilter dimensionFilter = new DimensionFilter();
        BeanUtils.copyProperties(pageDimensionReq, dimensionFilter);
        dimensionFilter.setModelIds(pageDimensionReq.getModelIds());
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
        Map<Long, String> modelFullPathMap = modelService.getModelFullPathMap();
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensionResps = dimensionDOS.stream()
                    .map(dimensionDO -> DimensionConverter.convert2DimensionResp(dimensionDO, modelFullPathMap,
                            new HashMap<>()))
                    .collect(Collectors.toList());
        }
        return dimensionResps;
    }

    @Override
    public List<DimensionResp> getDimensions(Long modelId) {
        return convertList(getDimensionDOS(modelId), datasourceService.getDatasourceMap());
    }

    @Override
    public List<DimensionResp> getDimensions() {
        return convertList(getDimensionDOS(), datasourceService.getDatasourceMap());
    }

    @Override
    public List<DimensionResp> getDimensionsByModelIds(List<Long> modelIds) {
        List<DimensionDO> dimensionDOS = dimensionRepository.getDimensionListOfmodelIds(modelIds);
        return convertList(dimensionDOS, datasourceService.getDatasourceMap());
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
        Map<Long, String> modelFullPathMap = modelService.getModelFullPathMap();
        if (!CollectionUtils.isEmpty(dimensionDOS)) {
            dimensionResps = dimensionDOS.stream()
                    .map(dimensionDO -> DimensionConverter.convert2DimensionResp(dimensionDO, modelFullPathMap,
                            datasourceRespMap))
                    .collect(Collectors.toList());
        }
        return dimensionResps;
    }


    @Override
    public List<DimensionResp> getHighSensitiveDimension(Long modelId) {
        List<DimensionResp> dimensionResps = getDimensions(modelId);
        if (CollectionUtils.isEmpty(dimensionResps)) {
            return dimensionResps;
        }
        return dimensionResps.stream()
                .filter(dimensionResp -> SensitiveLevelEnum.HIGH.getCode().equals(dimensionResp.getSensitiveLevel()))
                .collect(Collectors.toList());
    }


    protected List<DimensionDO> getDimensionDOS(Long modelId) {
        return dimensionRepository.getDimensionListOfmodel(modelId);
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
        //动态更新字典
        String type = DictWordType.DIMENSION.getType();
        applicationEventPublisher.publishEvent(
                new DataDeleteEvent(this, dimensionDO.getName(), dimensionDO.getModelId(), dimensionDO.getId(), type));

    }

    @Override
    public List<String> mockAlias(DimensionReq dimensionReq, String mockType, User user) {
        String mockAlias = chatGptHelper.mockAlias(mockType, dimensionReq.getName(), dimensionReq.getBizName(),
                "", dimensionReq.getDescription(), false);
        return JSONObject.parseObject(mockAlias, new TypeReference<List<String>>() {
        });
    }

    @Override
    public List<DimValueMap> mockDimensionValueAlias(DimensionReq dimensionReq, User user) {

        List<DatasourceResp> datasourceList = datasourceService.getDatasourceList();
        List<DatasourceResp> collect = datasourceList.stream().filter(datasourceResp ->
                datasourceResp.getId().equals(dimensionReq.getDatasourceId())).collect(Collectors.toList());

        if (collect.isEmpty()) {
            return null;
        }
        DatasourceResp datasourceResp = collect.get(0);
        DatasourceDetail datasourceDetail = datasourceResp.getDatasourceDetail();
        String sqlQuery = datasourceDetail.getSqlQuery();

        DatabaseResp database = databaseService.getDatabase(datasourceResp.getDatabaseId());

        String sql = "select ai_talk." + dimensionReq.getBizName() + " from (" + sqlQuery
                + ") as ai_talk group by ai_talk." + dimensionReq.getBizName();
        QueryResultWithSchemaResp queryResultWithSchemaResp = databaseService.executeSql(sql, database);
        List<Map<String, Object>> resultList = queryResultWithSchemaResp.getResultList();
        List<String> valueList = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : resultList) {
            String value = (String) stringObjectMap.get(dimensionReq.getBizName());
            valueList.add(value);
        }
        String json = chatGptHelper.mockDimensionValueAlias(JSON.toJSONString(valueList));
        log.info("return llm res is :{}", json);

        JSONObject jsonObject = JSON.parseObject(json);

        List<DimValueMap> dimValueMapsResp = new ArrayList<>();
        int i = 0;
        for (Map<String, Object> stringObjectMap : resultList) {
            DimValueMap dimValueMap = new DimValueMap();
            dimValueMap.setTechName(String.valueOf(stringObjectMap.get(dimensionReq.getBizName())));
            try {
                String tran = jsonObject.getJSONArray("tran").getString(i);
                dimValueMap.setBizName(tran);
            } catch (Exception exception) {
                dimValueMap.setBizName("");
            }
            try {
                dimValueMap.setAlias(jsonObject.getJSONObject("alias")
                        .getJSONArray(stringObjectMap.get(dimensionReq.getBizName()) + "").toJavaList(String.class));
            } catch (Exception exception) {
                dimValueMap.setAlias(null);
            }
            dimValueMapsResp.add(dimValueMap);
            i++;
        }
        return dimValueMapsResp;
    }


    private void checkExist(List<DimensionReq> dimensionReqs) {
        Long modelId = dimensionReqs.get(0).getModelId();
        List<DimensionResp> dimensionResps = getDimensions(modelId);
        for (DimensionReq dimensionReq : dimensionReqs) {
            if (NameCheckUtils.containsSpecialCharacters(dimensionReq.getName())) {
                throw new InvalidArgumentException("名称包含特殊字符, 请修改");
            }
            for (DimensionResp dimensionResp : dimensionResps) {
                if (dimensionResp.getName().equalsIgnoreCase(dimensionReq.getName())) {
                    throw new RuntimeException(String.format("存在相同的维度名 :%s", dimensionReq.getName()));
                }
                if (dimensionResp.getBizName().equalsIgnoreCase(dimensionReq.getBizName())) {
                    throw new RuntimeException(
                            String.format("存在相同的维度名: %s", dimensionReq.getBizName()));
                }
            }
        }
    }

}
