package com.tencent.supersonic.headless.server.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelRelaDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ModelRelaDOMapper;
import com.tencent.supersonic.headless.server.web.service.ModelRelaService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelRelaServiceImpl
        extends ServiceImpl<ModelRelaDOMapper, ModelRelaDO> implements ModelRelaService {

    @Override
    public void save(ModelRela modelRela, User user) {
        modelRela.createdBy(user.getName());
        ModelRelaDO modelRelaDO = convert(modelRela);
        save(modelRelaDO);
    }

    @Override
    public void update(ModelRela modelRela, User user) {
        modelRela.updatedBy(user.getName());
        ModelRelaDO modelRelaDO = convert(modelRela);
        updateById(modelRelaDO);
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
        modelRela.setJoinConditions(JSONObject.parseArray(modelRelaDO.getJoinCondition(), JoinCondition.class));
        return modelRela;
    }

    private ModelRelaDO convert(ModelRela modelRelaReq) {
        ModelRelaDO modelRelaDO = new ModelRelaDO();
        BeanMapper.mapper(modelRelaReq, modelRelaDO);
        modelRelaDO.setJoinCondition(JSONObject.toJSONString(modelRelaReq.getJoinConditions()));
        return modelRelaDO;
    }

}
