package com.tencent.supersonic.auth.authentication.infrastructure.repository;


import com.tencent.supersonic.auth.authentication.domain.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.domain.dataobject.UserDOExample;
import com.tencent.supersonic.auth.authentication.domain.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.infrastructure.mapper.UserDOMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryImpl implements UserRepository {


    private UserDOMapper userDOMapper;


    public UserRepositoryImpl(UserDOMapper userDOMapper) {
        this.userDOMapper = userDOMapper;
    }


    @Override
    public List<UserDO> getUserList() {
        return userDOMapper.selectByExample(new UserDOExample());
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


}
