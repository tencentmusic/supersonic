package com.tencent.supersonic.knowledge.infrastructure.custom;


import com.tencent.supersonic.knowledge.domain.dataobject.DictConfPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictConfMapper {

    Boolean createDictConf(DictConfPO dictConfPO);

    Boolean editDictConf(DictConfPO dictConfPO);

    Boolean upsertDictInfo(DictConfPO dictConfPO);

    DictConfPO getDictInfoByDomainId(Long domainId);
}
