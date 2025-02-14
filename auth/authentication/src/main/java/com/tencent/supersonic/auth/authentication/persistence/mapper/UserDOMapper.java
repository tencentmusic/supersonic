package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserDOMapper extends BaseMapper<UserDO> {

    List<UserDO> selectByExample(UserDOExample example);

    void updateByPrimaryKey(UserDO userDO);
}
