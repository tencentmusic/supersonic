package com.tencent.supersonic.knowledge.persistence.mapper;


import com.tencent.supersonic.knowledge.persistence.dataobject.DictConfDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictConfMapper {

    Boolean createDictConf(DictConfDO dictConfDO);

    Boolean editDictConf(DictConfDO dictConfDO);

    Boolean upsertDictInfo(DictConfDO dictConfDO);

    DictConfDO getDictInfoByModelId(Long modelId);
}
