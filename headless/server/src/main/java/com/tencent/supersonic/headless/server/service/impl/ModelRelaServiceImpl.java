package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelRelaDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ModelRelaDOMapper;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelRelaServiceImpl extends ServiceImpl<ModelRelaDOMapper, ModelRelaDO>
        implements ModelRelaService {

    @Lazy
    @Autowired
    private ModelService modelService;

    @Override
    public void save(ModelRela modelRela, User user) {
        check(modelRela);
        modelRela.createdBy(user.getName());
        ModelRelaDO modelRelaDO = convert(modelRela);
        save(modelRelaDO);
    }

    @Override
    public void update(ModelRela modelRela, User user) {
        check(modelRela);
        modelRela.updatedBy(user.getName());
        ModelRelaDO modelRelaDO = convert(modelRela);
        updateById(modelRelaDO);
    }

    private void check(ModelRela modelRela) {
        ModelResp fromModel = modelService.getModel(modelRela.getFromModelId());
        ModelResp toModel = modelService.getModel(modelRela.getToModelId());
        if (CollectionUtils.isEmpty(modelRela.getJoinConditions())) {
            throw new RuntimeException("关联关系不可为空");
        }
        for (JoinCondition joinCondition : modelRela.getJoinConditions()) {
            IdentifyType identifyTypeLeft = fromModel.getIdentifyType(joinCondition.getLeftField());
            IdentifyType identifyTypeRight = toModel.getIdentifyType(joinCondition.getRightField());
            if (IdentifyType.foreign.equals(identifyTypeLeft)
                    || IdentifyType.foreign.equals(identifyTypeRight)) {
                if (!IdentifyType.primary.equals(identifyTypeLeft)
                        && !IdentifyType.primary.equals(identifyTypeRight)) {
                    throw new RuntimeException("外键必须跟主键关联");
                }
            }
        }
    }

    @Override
    public List<ModelRela> getModelRelaList(Long domainId) {
        QueryWrapper<ModelRelaDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ModelRelaDO::getDomainId, domainId);
        return list(wrapper).stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public List<ModelRela> getModelRela(List<Long> modelIds) {
        QueryWrapper<ModelRelaDO> wrapper = new QueryWrapper<>();
        if (CollectionUtils.isEmpty(modelIds)) {
            return Lists.newArrayList();
        }
        wrapper.lambda().in(ModelRelaDO::getFromModelId, modelIds).or()
                .in(ModelRelaDO::getToModelId, modelIds);
        return list(wrapper).stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    private ModelRela convert(ModelRelaDO modelRelaDO) {
        ModelRela modelRela = new ModelRela();
        BeanMapper.mapper(modelRelaDO, modelRela);
        modelRela.setJoinConditions(
                JSONObject.parseArray(modelRelaDO.getJoinCondition(), JoinCondition.class));
        return modelRela;
    }

    private ModelRelaDO convert(ModelRela modelRelaReq) {
        ModelRelaDO modelRelaDO = new ModelRelaDO();
        BeanMapper.mapper(modelRelaReq, modelRelaDO);
        modelRelaDO.setJoinCondition(JSONObject.toJSONString(modelRelaReq.getJoinConditions()));
        return modelRelaDO;
    }
}
