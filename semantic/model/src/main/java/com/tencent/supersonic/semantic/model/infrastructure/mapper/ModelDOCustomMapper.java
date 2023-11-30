package com.tencent.supersonic.semantic.model.infrastructure.mapper;


import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ModelDOCustomMapper {

    void batchUpdateStatus(List<ModelDO> modelDOS);

}
