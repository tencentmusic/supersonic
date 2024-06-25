package com.tencent.supersonic.headless.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.server.persistence.dataobject.CollectDO;
import com.tencent.supersonic.headless.server.persistence.mapper.CollectMapper;
import com.tencent.supersonic.headless.server.web.service.CollectService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Slf4j
@Service
public class CollectServiceImpl implements CollectService {

    public static final String type = "metric";
    @Resource
    private CollectMapper collectMapper;

    @Override
    public Boolean collect(User user, CollectDO collectReq) {
        CollectDO collect = new CollectDO();
        collect.setType(Strings.isEmpty(collectReq.getType()) ? type : collectReq.getType());
        collect.setUsername(user.getName());
        collect.setCollectId(collectReq.getCollectId());
        collectMapper.insert(collect);
        return true;
    }

    @Override
    public Boolean unCollect(User user, Long id) {
        QueryWrapper<CollectDO> collectDOQueryWrapper = new QueryWrapper<>();
        collectDOQueryWrapper.lambda().eq(CollectDO::getUsername, user.getName());
        collectDOQueryWrapper.lambda().eq(CollectDO::getId, id);
        collectDOQueryWrapper.lambda().eq(CollectDO::getType, type);
        collectMapper.delete(collectDOQueryWrapper);
        return true;
    }

    @Override
    public Boolean unCollect(User user, CollectDO collectReq) {
        QueryWrapper<CollectDO> collectDOQueryWrapper = new QueryWrapper<>();
        collectDOQueryWrapper.lambda().eq(CollectDO::getUsername, user.getName());
        collectDOQueryWrapper.lambda().eq(CollectDO::getCollectId, collectReq.getCollectId());
        collectDOQueryWrapper.lambda().eq(CollectDO::getType, collectReq.getType());
        collectMapper.delete(collectDOQueryWrapper);
        return true;
    }

    @Override
    public List<CollectDO> getCollectionList(String username) {
        QueryWrapper<CollectDO> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(username)) {
            queryWrapper.lambda().eq(CollectDO::getUsername, username);
        }
        return collectMapper.selectList(queryWrapper);
    }

    @Override
    public List<CollectDO> getCollectionList(String username, TypeEnums typeEnums) {
        QueryWrapper<CollectDO> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(username)) {
            queryWrapper.lambda().eq(CollectDO::getUsername, username);
        }
        queryWrapper.lambda().eq(CollectDO::getType, typeEnums.name().toLowerCase());
        return collectMapper.selectList(queryWrapper);
    }

}
