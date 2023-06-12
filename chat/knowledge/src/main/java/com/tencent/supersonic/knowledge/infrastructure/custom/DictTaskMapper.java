package com.tencent.supersonic.knowledge.infrastructure.custom;

import com.tencent.supersonic.knowledge.domain.dataobject.DimValueDictTaskPO;
import com.tencent.supersonic.knowledge.domain.pojo.DictTaskFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictTaskMapper {

    Long createDimValueTask(DimValueDictTaskPO dimValueDictTaskPO);

    Boolean updateTaskStatus(DimValueDictTaskPO dimValueDictTaskPO);

    List<DimValueDictTaskPO> searchDictTaskList(DictTaskFilter filter);
}
