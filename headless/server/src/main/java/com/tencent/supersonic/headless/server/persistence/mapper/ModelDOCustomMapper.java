package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.server.persistence.dataobject.ModelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ModelDOCustomMapper {

    void batchUpdateStatus(List<ModelDO> modelDOS);
}
