package com.tencent.supersonic.semantic.model.application;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.ChatGptHelper;
import com.tencent.supersonic.semantic.api.model.pojo.DatasourceDetail;
import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.MetaBatchReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import com.tencent.supersonic.semantic.model.domain.repository.DimensionRepository;
import com.tencent.supersonic.semantic.model.domain.utils.DimensionConverter;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.pojo.DimensionFilter;
import java.util.Date;
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
    private ApplicationEventPublisher eventPublisher;


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
        dimensionReq.createdBy(user.getName());
        DimensionDO dimensionDO = DimensionConverter.convert2DimensionDO(dimensionReq);
        dimensionRepository.createDimension(dimensionDO);
        sendEventBatch(Lists.newArrayList(dimensionDO), EventType.ADD);
    }

    @Override
    public void createDimensionBatch(List<DimensionReq> dimensionReqs, User user) {
        if (CollectionUtils.isEmpty(dimensionReqs)) {
            return;
        }
        Long modelId = dimensionReqs.get(0).getModelId();
        List<DimensionResp> dimensionResps = getDimensions(modelId);
        Map<String, DimensionResp> bizNameMap = dimensionResps.stream()
                .collect(Collectors.toMap(DimensionResp::getBizName, a -> a, (k1, k2) -> k1));
        Map<String, DimensionResp> nameMap = dimensionResps.stream()
                .collect(Collectors.toMap(DimensionResp::getName, a -> a, (k1, k2) -> k1));
        List<DimensionReq> dimensionToInsert = dimensionReqs.stream()
                .filter(dimension -> !bizNameMap.containsKey(dimension.getBizName())
                        && !nameMap.containsKey(dimension.getName()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dimensionToInsert)) {
            return;
        }
        List<DimensionDO> dimensionDOS = dimensionToInsert.stream().peek(dimension ->
                        dimension.createdBy(user.getName()))
                .map(DimensionConverter::convert2DimensionDO)
                .collect(Collectors.toList());
        dimensionRepository.createDimensionBatch(dimensionDOS);
        sendEventBatch(dimensionDOS, EventType.ADD);
    }

    @Override
    public void updateDimension(DimensionReq dimensionReq, User user) {
        checkExist(Lists.newArrayList(dimensionReq));
        DimensionDO dimensionDO = dimensionRepository.getDimensionById(dimensionReq.getId());
        dimensionReq.updatedBy(user.getName());
        String oldName = dimensionDO.getName();
        DimensionConverter.convert(dimensionDO, dimensionReq);
        dimensionRepository.updateDimension(dimensionDO);
        if (!oldName.equals(dimensionDO.getName())) {
            sendEvent(DataItem.builder().modelId(dimensionDO.getModelId()).newName(dimensionReq.getName())
                    .name(oldName).type(TypeEnums.DIMENSION)
                    .id(dimensionDO.getId()).build(), EventType.UPDATE);
        }
    }

    @Override
    public void batchUpdateStatus(MetaBatchReq metaBatchReq, User user) {
        if (CollectionUtils.isEmpty(metaBatchReq.getIds())) {
            return;
        }
        DimensionFilter dimensionFilter = new DimensionFilter();
        dimensionFilter.setIds(metaBatchReq.getIds());
        List<DimensionDO> dimensionDOS = dimensionRepository.getDimension(dimensionFilter);
        if (CollectionUtils.isEmpty(dimensionDOS)) {
            return;
        }
        dimensionDOS = dimensionDOS.stream()
                .peek(dimensionDO -> {
                    dimensionDO.setStatus(metaBatchReq.getStatus());
                    dimensionDO.setUpdatedAt(new Date());
                    dimensionDO.setUpdatedBy(user.getName());
                })
                .collect(Collectors.toList());
        dimensionRepository.batchUpdateStatus(dimensionDOS);
        if (StatusEnum.OFFLINE.getCode().equals(metaBatchReq.getStatus())
                || StatusEnum.DELETED.getCode().equals(metaBatchReq.getStatus())) {
            sendEventBatch(dimensionDOS, EventType.DELETE);
        } else if (StatusEnum.ONLINE.getCode().equals(metaBatchReq.getStatus())) {
            sendEventBatch(dimensionDOS, EventType.ADD);
        }
    }

    @Override
    public void deleteDimension(Long id, User user) {
        DimensionDO dimensionDO = dimensionRepository.getDimensionById(id);
        if (dimensionDO == null) {
            throw new RuntimeException(String.format("the dimension %s not exist", id));
        }
        dimensionDO.setStatus(StatusEnum.DELETED.getCode());
        dimensionDO.setUpdatedAt(new Date());
        dimensionDO.setUpdatedBy(user.getName());
        dimensionRepository.updateDimension(dimensionDO);
        sendEventBatch(Lists.newArrayList(dimensionDO), EventType.DELETE);
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
    public List<DimensionResp> getDimensions(MetaFilter metaFilter) {
        DimensionFilter dimensionFilter = new DimensionFilter();
        BeanUtils.copyProperties(metaFilter, dimensionFilter);
        return convertList(dimensionRepository.getDimension(dimensionFilter), datasourceService.getDatasourceMap());
    }

    private List<DimensionResp> getDimensions(Long modelId) {
        return getDimensions(new MetaFilter(Lists.newArrayList(modelId)));
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
    public List<DataItem> getDataItems(Long modelId) {
        DimensionFilter metaFilter = new DimensionFilter();
        metaFilter.setModelIds(Lists.newArrayList(modelId));
        List<DimensionDO> dimensionDOS = queryDimension(metaFilter);
        if (CollectionUtils.isEmpty(dimensionDOS)) {
            return Lists.newArrayList();
        }
        return dimensionDOS.stream().map(this::getDataItem).collect(Collectors.toList());
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
        Map<String, DimensionResp> bizNameMap = dimensionResps.stream()
                .collect(Collectors.toMap(DimensionResp::getBizName, a -> a, (k1, k2) -> k1));
        Map<String, DimensionResp> nameMap = dimensionResps.stream()
                .collect(Collectors.toMap(DimensionResp::getName, a -> a, (k1, k2) -> k1));
        for (DimensionReq dimensionReq : dimensionReqs) {
            if (NameCheckUtils.containsSpecialCharacters(dimensionReq.getName())) {
                throw new InvalidArgumentException("名称包含特殊字符, 请修改");
            }
            if (bizNameMap.containsKey(dimensionReq.getBizName())) {
                DimensionResp dimensionResp = bizNameMap.get(dimensionReq.getBizName());
                if (!dimensionResp.getId().equals(dimensionReq.getId())) {
                    throw new RuntimeException(String.format("该模型下存在相同的维度字段名:%s 创建人:%s",
                            dimensionReq.getBizName(), dimensionResp.getCreatedBy()));
                }
            }
            if (nameMap.containsKey(dimensionReq.getName())) {
                DimensionResp dimensionResp = nameMap.get(dimensionReq.getName());
                if (!dimensionResp.getId().equals(dimensionReq.getId())) {
                    throw new RuntimeException(String.format("该模型下存在相同的维度名:%s 创建人:%s",
                            dimensionReq.getName(), dimensionResp.getCreatedBy()));
                }
            }
        }
    }

    private void sendEventBatch(List<DimensionDO> dimensionDOS, EventType eventType) {
        List<DataItem> dataItems = dimensionDOS.stream().map(dimensionDO ->
                        DataItem.builder().id(dimensionDO.getId()).name(dimensionDO.getName())
                                .modelId(dimensionDO.getModelId()).type(TypeEnums.DIMENSION).build())
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new DataEvent(this, dataItems, eventType));
    }

    private void sendEvent(DataItem dataItem, EventType eventType) {
        eventPublisher.publishEvent(new DataEvent(this,
                Lists.newArrayList(dataItem), eventType));
    }

    private DataItem getDataItem(DimensionDO dimensionDO) {
        return DataItem.builder().id(dimensionDO.getId()).name(dimensionDO.getName())
                .bizName(dimensionDO.getBizName())
                .modelId(dimensionDO.getModelId()).type(TypeEnums.DIMENSION).build();
    }


}
