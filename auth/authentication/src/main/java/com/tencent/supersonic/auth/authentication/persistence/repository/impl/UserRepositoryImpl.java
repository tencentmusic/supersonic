package com.tencent.supersonic.auth.authentication.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDOExample;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserTokenDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserTokenDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryImpl implements UserRepository {

    private UserDOMapper userDOMapper;

    private UserTokenDOMapper userTokenDOMapper;

    public UserRepositoryImpl(UserDOMapper userDOMapper, UserTokenDOMapper userTokenDOMapper) {
        this.userDOMapper = userDOMapper;
        this.userTokenDOMapper = userTokenDOMapper;
    }

    @Override
    public List<UserDO> getUserList() {
        return userDOMapper.selectByExample(new UserDOExample());
    }

    @Override
    public void updateUser(UserDO userDO) {
        userDOMapper.updateByPrimaryKey(userDO);
    }

    @Override
    public void addUser(UserDO userDO) {
        userDOMapper.insert(userDO);
    }

    @Override
    public UserDO getUser(String name) {
        UserDOExample userDOExample = new UserDOExample();
        userDOExample.createCriteria().andNameEqualTo(name);
        List<UserDO> userDOS = userDOMapper.selectByExample(userDOExample);
        Optional<UserDO> userDOOptional = userDOS.stream().findFirst();
        return userDOOptional.orElse(null);
    }

    @Override
    public List<UserTokenDO> getUserTokenListByName(String userName) {
        QueryWrapper<UserTokenDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserTokenDO::getUserName, userName);
        return userTokenDOMapper.selectList(queryWrapper);
    }

    @Override
    public void addUserToken(UserTokenDO userTokenDO) {
        userTokenDOMapper.insert(userTokenDO);
    }

    @Override
    public UserTokenDO getUserToken(Long tokenId) {
        return userTokenDOMapper.selectById(tokenId);
    }

    @Override
    public void deleteUserTokenByName(String userName) {
        QueryWrapper<UserTokenDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserTokenDO::getUserName, userName);
        userTokenDOMapper.delete(queryWrapper);
    }

    @Override
    public void deleteUserToken(Long tokenId) {
        userTokenDOMapper.deleteById(tokenId);
    }
}
