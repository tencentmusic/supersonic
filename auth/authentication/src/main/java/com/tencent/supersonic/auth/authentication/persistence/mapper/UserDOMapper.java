package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserDOMapper {

    /** @mbg.generated */
    int insert(UserDO record);

    /** @mbg.generated */
    List<UserDO> selectByExample(UserDOExample example);

    void updateByPrimaryKey(UserDO userDO);
}
