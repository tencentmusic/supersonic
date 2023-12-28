package com.tencent.supersonic.headless.materialization.infrastructure.mapper;

import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationFilter;
import com.tencent.supersonic.headless.materialization.domain.dataobject.MaterializationDOWithBLOBs;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MaterializationDOCustomMapper {

    List<MaterializationDOWithBLOBs> getMaterializationResp(MaterializationFilter filter);
}
