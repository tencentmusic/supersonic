package com.tencent.supersonic.auth.authentication.infrastructure.mapper;


import com.tencent.supersonic.auth.authentication.domain.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.domain.dataobject.UserDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDOMapper {

    /**
     * @mbg.generated
     */
    long countByExample(UserDOExample example);

    /**
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     * @mbg.generated
     */
    int insert(UserDO record);

    /**
     * @mbg.generated
     */
    int insertSelective(UserDO record);

    /**
     * @mbg.generated
     */
    List<UserDO> selectByExample(UserDOExample example);

    /**
     * @mbg.generated
     */
    UserDO selectByPrimaryKey(Long id);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(UserDO record);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKey(UserDO record);
}