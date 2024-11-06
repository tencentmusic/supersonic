package com.tencent.supersonic.headless.server.service.impl;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.TagFilterPageReq;
import com.tencent.supersonic.headless.api.pojo.request.TagObjectReq;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.TagObjectResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagObjectDO;
import com.tencent.supersonic.headless.server.persistence.repository.TagObjectRepository;
import com.tencent.supersonic.headless.server.pojo.TagObjectFilter;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.TagMetaService;
import com.tencent.supersonic.headless.server.service.TagObjectService;
import com.tencent.supersonic.headless.server.utils.TagObjectConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TagObjectServiceImpl implements TagObjectService {

    private final TagObjectRepository tagObjectRepository;
    private final ModelService modelService;
    private final TagMetaService tagMetaService;

    public TagObjectServiceImpl(TagObjectRepository tagObjectRepository, ModelService modelService,
            @Lazy TagMetaService tagMetaService) {
        this.tagObjectRepository = tagObjectRepository;
        this.modelService = modelService;
        this.tagMetaService = tagMetaService;
    }

    @Override
    public TagObjectResp create(TagObjectReq tagObjectReq, User user) throws Exception {
        checkParam(tagObjectReq, user);

        TagObjectDO tagObjectDO = TagObjectConverter.convert(tagObjectReq);
        Date date = new Date();
        tagObjectDO.setCreatedBy(user.getName());
        tagObjectDO.setCreatedAt(date);
        tagObjectDO.setUpdatedBy(user.getName());
        tagObjectDO.setUpdatedAt(date);
        tagObjectDO.setStatus(StatusEnum.ONLINE.getCode());
        tagObjectRepository.create(tagObjectDO);
        TagObjectDO tagObjectById = tagObjectRepository.getTagObjectById(tagObjectDO.getId());
        return TagObjectConverter.convert2Resp(tagObjectById);
    }

    private void checkParam(TagObjectReq tagObjectReq, User user) throws Exception {
        TagObjectFilter filter = new TagObjectFilter();
        filter.setDomainId(tagObjectReq.getDomainId());
        List<TagObjectResp> tagObjectRespList = getTagObjects(filter, user);
        if (CollectionUtils.isEmpty(tagObjectRespList)) {
            return;
        }
        tagObjectRespList = tagObjectRespList.stream().filter(
                tagObjectResp -> StatusEnum.ONLINE.getCode().equals(tagObjectResp.getStatus()))
                .collect(Collectors.toList());
        for (TagObjectResp tagObject : tagObjectRespList) {
            if (tagObject.getBizName().equalsIgnoreCase(tagObjectReq.getBizName())) {
                throw new Exception(
                        String.format("the bizName %s is exist", tagObjectReq.getBizName()));
            }
            if (tagObject.getName().equalsIgnoreCase(tagObjectReq.getName())) {
                throw new Exception(String.format("the name %s is exist", tagObjectReq.getName()));
            }
        }
    }

    @Override
    public TagObjectResp update(TagObjectReq tagObjectReq, User user) {
        TagObjectDO tagObjectDO = tagObjectRepository.getTagObjectById(tagObjectReq.getId());
        BeanMapper.mapper(tagObjectReq, tagObjectDO);
        tagObjectDO.setUpdatedAt(new Date());
        tagObjectDO.setUpdatedBy(user.getName());
        tagObjectRepository.update(tagObjectDO);
        TagObjectDO tagObjectById = tagObjectRepository.getTagObjectById(tagObjectReq.getId());
        return TagObjectConverter.convert2Resp(tagObjectById);
    }

    @Override
    public Boolean delete(Long id, User user) throws Exception {
        TagObjectDO tagObjectDO = tagObjectRepository.getTagObjectById(id);
        checkDeletePermission(tagObjectDO, user);
        checkTagObjectStatus(tagObjectDO, user);
        tagObjectDO.setUpdatedAt(new Date());
        tagObjectDO.setUpdatedBy(user.getName());
        tagObjectDO.setStatus(StatusEnum.DELETED.getCode());
        tagObjectRepository.update(tagObjectDO);
        return true;
    }

    @Override
    public Boolean delete(Long id, User user, Boolean checkStatus) throws Exception {
        TagObjectDO tagObjectDO = tagObjectRepository.getTagObjectById(id);
        checkDeletePermission(tagObjectDO, user);
        if (checkStatus) {
            checkTagObjectStatus(tagObjectDO, user);
        }
        tagObjectDO.setUpdatedAt(new Date());
        tagObjectDO.setUpdatedBy(user.getName());
        tagObjectDO.setStatus(StatusEnum.DELETED.getCode());
        tagObjectRepository.update(tagObjectDO);
        return true;
    }

    private void checkTagObjectStatus(TagObjectDO tagObjectDO, User user) throws Exception {
        List<ModelResp> allModelByDomainIds =
                modelService.getAllModelByDomainIds(Arrays.asList(tagObjectDO.getDomainId()));
        if (!CollectionUtils.isEmpty(allModelByDomainIds)) {
            List<Long> modelIds =
                    allModelByDomainIds.stream().map(ModelResp::getId).collect(Collectors.toList());
            throw new Exception("delete operation is not supported at the moment. related modelIds:"
                    + modelIds);
        }
        TagFilterPageReq tagMarketPageReq = new TagFilterPageReq();
        tagMarketPageReq.setTagObjectId(tagObjectDO.getId());
        PageInfo<TagResp> respPageInfo = tagMetaService.queryTagMarketPage(tagMarketPageReq, user);
        if (Objects.nonNull(respPageInfo)) {
            List<TagResp> tagRespList = respPageInfo.getList();
            if (!CollectionUtils.isEmpty(tagRespList)) {
                List<Long> tagIds =
                        tagRespList.stream().map(TagResp::getId).collect(Collectors.toList());
                throw new Exception(
                        "delete operation is not supported at the moment. related tagIds:"
                                + tagIds);
            }
        }
    }

    private void checkDeletePermission(TagObjectDO tagObjectDO, User user) throws Exception {
        if (user.getName().equalsIgnoreCase(tagObjectDO.getCreatedBy()) || user.isSuperAdmin()) {
            return;
        }
        throw new Exception(
                "delete operation is not supported at the moment. Please contact the admin.");
    }

    @Override
    public TagObjectResp getTagObject(Long id, User user) {
        TagObjectDO tagObjectDO = tagObjectRepository.getTagObjectById(id);
        return TagObjectConverter.convert2Resp(tagObjectDO);
    }

    @Override
    public List<TagObjectResp> getTagObjects(TagObjectFilter filter, User user) {
        List<TagObjectDO> tagObjectDOList = tagObjectRepository.query(filter);
        return TagObjectConverter.convert2RespList(tagObjectDOList);
    }

    @Override
    public Map<Long, TagObjectResp> getAllTagObjectMap() {
        TagObjectFilter filter = new TagObjectFilter();
        List<TagObjectDO> tagObjectDOList = tagObjectRepository.query(filter);
        List<TagObjectResp> tagObjectRespList =
                TagObjectConverter.convert2RespList(tagObjectDOList);
        Map<Long, TagObjectResp> map = tagObjectRespList.stream()
                .collect(Collectors.toMap(TagObjectResp::getId, a -> a, (k1, k2) -> k1));
        return map;
    }
}
