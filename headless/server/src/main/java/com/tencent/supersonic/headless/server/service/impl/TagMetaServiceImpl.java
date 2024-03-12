package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.TagDefineParams;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.TagBatchCreateReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.CollectDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.persistence.repository.TagRepository;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import com.tencent.supersonic.headless.server.pojo.TagFilterPage;
import com.tencent.supersonic.headless.server.service.CollectService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.TagMetaService;
import com.tencent.supersonic.headless.server.utils.NameCheckUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TagMetaServiceImpl implements TagMetaService {

    private final TagRepository tagRepository;
    private final ModelService modelService;
    private final CollectService collectService;
    private ApplicationEventPublisher eventPublisher;
    private final DimensionService dimensionService;
    private final MetricService metricService;

    public TagMetaServiceImpl(TagRepository tagRepository, ModelService modelService,
                              CollectService collectService, ApplicationEventPublisher eventPublisher,
                              @Lazy DimensionService dimensionService, @Lazy MetricService metricService) {
        this.tagRepository = tagRepository;
        this.modelService = modelService;
        this.collectService = collectService;
        this.eventPublisher = eventPublisher;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
    }

    @Override
    public TagResp create(TagReq tagReq, User user) {
        checkParam(tagReq);
        checkExit(tagReq);
        TagDO tagDO = convert(tagReq);
        Date date = new Date();
        tagDO.setCreatedBy(user.getName());
        tagDO.setCreatedAt(date);
        tagDO.setUpdatedBy(user.getName());
        tagDO.setUpdatedAt(date);
        if (Objects.nonNull(tagReq.getStatus())) {
            tagDO.setStatus(tagReq.getStatus());
        } else {
            tagDO.setStatus(StatusEnum.ONLINE.getCode());
        }
        tagRepository.create(tagDO);
        sendEventBatch(Lists.newArrayList(tagDO), EventType.ADD);
        return convert(tagDO);
    }

    private void sendEventBatch(List<TagDO> tagDOS, EventType eventType) {
        List<DataItem> dataItems = tagDOS.stream().map(this::getDataItem)
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new DataEvent(this, dataItems, eventType));
    }

    private void sendEvent(DataItem dataItem, EventType eventType) {
        eventPublisher.publishEvent(new DataEvent(this,
                Lists.newArrayList(dataItem), eventType));
    }

    private DataItem getDataItem(TagDO tagDO) {
        return DataItem.builder().id(tagDO.getId() + Constants.UNDERLINE).name(tagDO.getName())
                .bizName(tagDO.getBizName()).modelId(tagDO.getModelId() + Constants.UNDERLINE)
                .type(TypeEnums.TAG).build();
    }

    @Override
    public TagResp update(TagReq tagReq, User user) {
        if (Objects.isNull(tagReq.getId()) || tagReq.getId() <= 0) {
            throw new RuntimeException("id is empty");
        }
        TagDO tagDO = tagRepository.getTagById(tagReq.getId());
        String oldName = tagDO.getName();
        tagDO = fillUpdateInfo(tagReq, tagDO);
        tagDO.setUpdatedBy(user.getName());
        tagDO.setUpdatedAt(new Date());
        tagRepository.update(tagDO);
        if (!oldName.equals(tagReq.getName())) {
            DataItem dataItem = getDataItem(tagDO);
            dataItem.setName(oldName);
            dataItem.setNewName(tagReq.getName());
            sendEvent(getDataItem(tagDO), EventType.UPDATE);
        }
        return convert(tagDO);
    }

    @Override
    public void delete(Long id, User user) {
        TagDO tagDO = tagRepository.getTagById(id);
        if (Objects.isNull(tagDO)) {
            throw new RuntimeException("tag not found");
        }
        tagDO.setStatus(StatusEnum.DELETED.getCode());
        tagDO.setUpdatedBy(user.getName());
        tagDO.setUpdatedAt(new Date());
        tagRepository.update(tagDO);
        sendEventBatch(Lists.newArrayList(tagDO), EventType.DELETE);
    }

    @Override
    public TagResp getTag(Long id, User user) {
        // return convert(tagRepository.getTagById(id));
        TagDO tagDO = tagRepository.getTagById(id);
        TagResp tagResp = fillCollectAndAdminInfo(tagDO, user);
        tagResp = fillModelInfo(tagResp);
        return tagResp;
    }

    @Override
    public List<TagResp> getTags(TagFilter tagFilter) {
        List<TagDO> tagDOS = tagRepository.query(tagFilter);
        if (!CollectionUtils.isEmpty(tagDOS)) {
            return tagDOS.stream().map(tagDO -> convert(tagDO)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public PageInfo<TagResp> queryPage(TagFilterPage tagFilterPage, User user) {
        TagFilter tagFilter = new TagFilter();
        BeanUtils.copyProperties(tagFilterPage, tagFilter);
        List<ModelResp> modelRespList = modelService.getAllModelByDomainIds(tagFilterPage.getDomainIds());
        List<Long> modelIds = modelRespList.stream().map(ModelResp::getId).collect(Collectors.toList());
        tagFilterPage.getModelIds().addAll(modelIds);
        tagFilter.setModelIds(tagFilterPage.getModelIds());

        List<CollectDO> collectList = collectService.getCollectList(user.getName())
                .stream().filter(collectDO -> TypeEnums.TAG.name().equalsIgnoreCase(collectDO.getType()))
                .collect(Collectors.toList());
        List<Long> collectIds = collectList.stream().map(CollectDO::getCollectId).collect(Collectors.toList());
        if (tagFilterPage.isHasCollect()) {
            if (CollectionUtils.isEmpty(collectIds)) {
                tagFilter.setIds(Lists.newArrayList(-1L));
            } else {
                tagFilter.setIds(collectIds);
            }
        }

        PageInfo<TagDO> tagDOPageInfo = PageHelper.startPage(tagFilterPage.getCurrent(),
                tagFilterPage.getPageSize())
                .doSelectPageInfo(() -> getTags(tagFilter));
        PageInfo<TagResp> pageInfo = new PageInfo<>();
        BeanUtils.copyProperties(tagDOPageInfo, pageInfo);
        List<TagResp> tagRespList = convertList(tagDOPageInfo.getList(), collectIds);
        fillAdminRes(tagRespList, user);
        fillModelInfo(tagRespList);
        pageInfo.setList(tagRespList);

        return pageInfo;
    }

    @Override
    public Boolean batchUpdateStatus(MetaBatchReq metaBatchReq, User user) {
        if (Objects.isNull(metaBatchReq)) {
            return false;
        }
        TagFilter tagFilter = new TagFilter();
        BeanUtils.copyProperties(metaBatchReq, tagFilter);
        tagFilter.setStatus(null);
        List<TagDO> tagDOList = tagRepository.query(tagFilter);
        log.info("tagFilter:{},{}", tagFilter.getModelIds(), tagFilter.getBizNames());
        if (CollectionUtils.isEmpty(tagDOList)) {
            return true;
        }
        tagDOList.stream().forEach(tagDO -> {
            tagDO.setStatus(metaBatchReq.getStatus());
            tagDO.setUpdatedAt(new Date());
            tagDO.setUpdatedBy(user.getName());
        });

        tagRepository.batchUpdateStatus(tagDOList);
        if (StatusEnum.OFFLINE.getCode().equals(metaBatchReq.getStatus())
                || StatusEnum.DELETED.getCode().equals(metaBatchReq.getStatus())) {
            sendEventBatch(tagDOList, EventType.DELETE);
        } else if (StatusEnum.ONLINE.getCode().equals(metaBatchReq.getStatus())) {
            sendEventBatch(tagDOList, EventType.ADD);
        }
        return true;
    }

    @Override
    public Integer createBatch(TagBatchCreateReq tagLoadReq, User user) {
        Long modelId = tagLoadReq.getModelId();
        int num = 0;
        if (Objects.isNull(tagLoadReq.getType()) || SchemaElementType.DIMENSION.equals(tagLoadReq.getType())) {
            List<DimensionResp> dimensions = dimensionService.getDimensionInModelCluster(modelId);
            num += loadDimTagBatch(tagLoadReq, dimensions, user);
        }
        if (Objects.isNull(tagLoadReq.getType()) || SchemaElementType.METRIC.equals(tagLoadReq.getType())) {
            MetaFilter metaFilter = new MetaFilter();
            List<Long> modelIds = new ArrayList<>();
            modelIds.add(modelId);
            List<MetricResp> metrics = metricService.getMetrics(metaFilter);
            num += loadMetricTagBatch(tagLoadReq, metrics, user);
        }
        log.info("loadTagBatch finished ,tag num:{}", num);
        return num;
    }

    private int loadMetricTagBatch(TagBatchCreateReq tagLoadReq, List<MetricResp> metrics, User user) {
        if (!CollectionUtils.isEmpty(tagLoadReq.getItemIds())) {
            metrics = metrics.stream().filter(metric -> tagLoadReq.getItemIds().contains(metric.getId()))
                    .collect(Collectors.toList());
        }
        metrics.stream().forEach(metric -> {
            TagReq tagReq = new TagReq();
            BeanUtils.copyProperties(metric, tagReq);
            tagReq.setId(null);
            tagReq.setBizName(metric.getBizName());
            tagReq.setTagDefineType(TagDefineType.METRIC);
            TagDefineParams tagDefineParams = new TagDefineParams();
            tagDefineParams.setExpr(metric.getBizName());
            tagDefineParams.setDependencies(new ArrayList<>(Arrays.asList(metric.getId())));
            // tagReq.setSensitiveLevel(metric.getSensitiveLevel());
            tagReq.setTagDefineParams(tagDefineParams);
            create(tagReq, user);
        });
        return metrics.size();
    }

    private Integer loadDimTagBatch(TagBatchCreateReq tagLoadReq, List<DimensionResp> dimensions, User user) {
        if (!CollectionUtils.isEmpty(tagLoadReq.getItemIds())) {
            dimensions = dimensions.stream().filter(dim -> tagLoadReq.getItemIds().contains(dim.getId()))
                    .collect(Collectors.toList());
        }
        dimensions.stream().forEach(dim -> {
            TagReq tagReq = new TagReq();
            BeanUtils.copyProperties(dim, tagReq);
            tagReq.setId(null);
            tagReq.setBizName(dim.getBizName());
            tagReq.setTagDefineType(TagDefineType.DIMENSION);
            TagDefineParams tagDefineParams = new TagDefineParams();
            tagDefineParams.setExpr(dim.getBizName());
            tagDefineParams.setDependencies(new ArrayList<>(Arrays.asList(dim.getId())));
            // tagReq.setSensitiveLevel(dim.getSensitiveLevel());
            tagReq.setTagDefineParams(tagDefineParams);
            try {
                create(tagReq, user);
            } catch (Exception e) {
                log.info("loadDimTagBatch, e:{}", e);
            }

        });
        return dimensions.size();
    }

    private TagDO fillUpdateInfo(TagReq tagReq, TagDO tagDO) {
        if (Objects.nonNull(tagDO) && tagDO.getId() > 0) {
            if (Objects.nonNull(tagReq.getExt()) && !tagReq.getExt().isEmpty()) {
                tagDO.setExt(tagReq.getExtJson());
            }
        }
        if (Objects.nonNull(tagReq.getTagDefineType())) {
            tagDO.setDefineType(tagReq.getTagDefineType().name());
        }
        if (Objects.nonNull(tagReq.getTagDefineParams()) && !StringUtils.isBlank(
                tagReq.getTagDefineParams().getExpr())) {
            tagDO.setTypeParams(tagReq.getTypeParamsJson());
        }
        if (Strings.isNotEmpty(tagReq.getDescription())) {
            tagDO.setDescription(tagReq.getDescription());
        }
        if (Objects.nonNull(tagReq.getSensitiveLevel())) {
            tagDO.setSensitiveLevel(tagReq.getSensitiveLevel());
        }
        if (Strings.isNotEmpty(tagReq.getName())) {
            tagDO.setName(tagReq.getName());
        }
        if (Objects.nonNull(tagReq.getStatus())) {
            tagDO.setStatus(tagReq.getStatus());
        }

        return tagDO;
    }

    private TagResp fillModelInfo(TagResp tagResp) {
        ModelResp model = modelService.getModel(tagResp.getModelId());
        tagResp.setModelName(model.getName());
        tagResp.setDomainId(model.getDomainId());
        return tagResp;
    }

    private void fillModelInfo(List<TagResp> tagRespList) {
        Map<Long, ModelResp> modelIdAndRespMap = modelService.getModelMap();
        tagRespList.stream().forEach(tagResp -> {
            if (Objects.nonNull(modelIdAndRespMap) && modelIdAndRespMap.containsKey(tagResp.getModelId())) {
                tagResp.setModelName(modelIdAndRespMap.get(tagResp.getModelId()).getName());
                tagResp.setDomainId(modelIdAndRespMap.get(tagResp.getModelId()).getDomainId());
            }
        });
    }

    private TagResp fillCollectAndAdminInfo(TagDO tagDO, User user) {
        List<Long> collectIds = collectService.getCollectList(user.getName())
                .stream().filter(collectDO -> TypeEnums.TAG.name().equalsIgnoreCase(collectDO.getType()))
                .map(CollectDO::getCollectId).collect(Collectors.toList());

        List<TagResp> tagRespList = convertList(new ArrayList<>(Arrays.asList(tagDO)), collectIds);
        fillAdminRes(tagRespList, user);
        return tagRespList.get(0);
    }

    private void fillAdminRes(List<TagResp> tagRespList, User user) {
        List<ModelResp> modelRespList = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        if (CollectionUtils.isEmpty(modelRespList)) {
            return;
        }
        Set<Long> modelIdSet = modelRespList.stream().map(ModelResp::getId).collect(Collectors.toSet());
        for (TagResp tagResp : tagRespList) {
            if (modelIdSet.contains(tagResp.getModelId())) {
                tagResp.setHasAdminRes(true);
            } else {
                tagResp.setHasAdminRes(false);
            }
        }
    }

    private List<TagResp> convertList(List<TagDO> tagDOList, List<Long> collectIds) {
        List<TagResp> tagRespList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tagDOList)) {
            tagDOList.stream().forEach(tagDO -> {
                TagResp tagResp = convert(tagDO);
                if (CollectionUtils.isNotEmpty(collectIds) && collectIds.contains(tagDO.getId())) {
                    tagResp.setIsCollect(true);
                } else {
                    tagResp.setIsCollect(false);
                }
                tagRespList.add(tagResp);
            });
        }
        return tagRespList;
    }

    private void checkExit(TagReq tagReq) {
        TagFilter tagFilter = new TagFilter();
        tagFilter.setModelIds(Arrays.asList(tagReq.getModelId()));

        List<TagResp> tagResps = getTags(tagFilter);
        if (!CollectionUtils.isEmpty(tagResps)) {
            Long bizNameSameCount = tagResps.stream().filter(tagResp -> !tagResp.getId().equals(tagReq.getId()))
                    .filter(tagResp -> tagResp.getBizName().equalsIgnoreCase(tagReq.getBizName())).count();
            if (bizNameSameCount > 0) {
                throw new RuntimeException(String.format("the bizName %s is exit", tagReq.getBizName()));
            }
            Long nameSameCount = tagResps.stream().filter(tagResp -> !tagResp.getId().equals(tagReq.getId()))
                    .filter(tagResp -> tagResp.getName().equalsIgnoreCase(tagReq.getName())).count();
            if (nameSameCount > 0) {
                throw new RuntimeException(String.format("the name %s is exit", tagReq.getName()));
            }
        }
    }

    private void checkParam(TagReq tagReq) {
        if (Objects.isNull(tagReq.getModelId()) || tagReq.getModelId() <= 0) {
            throw new RuntimeException("the modelId is empty");
        }
        if (Objects.isNull(tagReq.getBizName()) || tagReq.getBizName().isEmpty() || Objects.isNull(tagReq.getName())
                || tagReq.getName().isEmpty()) {
            throw new RuntimeException("the bizName or name is empty");
        }
        if (Objects.isNull(tagReq.getTagDefineType()) || Objects.isNull(tagReq.getTagDefineParams())
                || StringUtils.isBlank(tagReq.getTagDefineParams().getExpr())) {
            throw new InvalidArgumentException("表达式不可为空");
        }

        if (NameCheckUtils.containsSpecialCharacters(tagReq.getBizName())) {
            throw new InvalidArgumentException("名称包含特殊字符, 请修改");
        }
    }

    private TagResp convert(TagDO tagDO) {
        TagResp tagResp = new TagResp();
        BeanUtils.copyProperties(tagDO, tagResp);
        if (Objects.nonNull(tagDO.getExt()) && !tagDO.getExt().isEmpty()) {
            Map<String, Object> ext = JSONObject.parseObject(tagDO.getExt(),
                    Map.class);
            tagResp.setExt(ext);
        }
        tagResp.setTagDefineType(TagDefineType.valueOf(tagDO.getDefineType()));
        if (Objects.nonNull(tagDO.getTypeParams()) && !tagDO.getTypeParams().isEmpty()) {
            TagDefineParams tagDefineParams = JSONObject.parseObject(tagDO.getTypeParams(),
                    TagDefineParams.class);
            tagResp.setTagDefineParams(tagDefineParams);
        }

        return tagResp;
    }

    private TagDO convert(TagReq tagReq) {
        TagDO tagDO = new TagDO();
        BeanUtils.copyProperties(tagReq, tagDO);
        tagDO.setDefineType(tagReq.getTagDefineType().name());
        tagDO.setType(tagReq.getType().name());
        tagDO.setTypeParams(tagReq.getTypeParamsJson());
        tagDO.setExt(tagReq.getExtJson());
        return tagDO;
    }
}
