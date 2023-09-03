package com.tencent.supersonic.chat.persistence.mapper;

import com.tencent.supersonic.chat.persistence.dataobject.PluginDO;
import com.tencent.supersonic.chat.persistence.dataobject.PluginDOExample;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PluginDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(PluginDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int insert(PluginDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(PluginDO record);

    /**
     *
     * @mbg.generated
     */
    List<PluginDO> selectByExampleWithBLOBs(PluginDOExample example);

    /**
     *
     * @mbg.generated
     */
    List<PluginDO> selectByExample(PluginDOExample example);

    /**
     *
     * @mbg.generated
     */
    PluginDO selectByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(PluginDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(PluginDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(PluginDO record);
}