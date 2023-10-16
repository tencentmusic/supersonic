package com.tencent.supersonic.semantic.materialization.infrastructure.mapper;

import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationFilter;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationDOWithBLOBs;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MaterializationDOCustomMapper {

    List<MaterializationDOWithBLOBs> getMaterializationResp(MaterializationFilter filter);
}
