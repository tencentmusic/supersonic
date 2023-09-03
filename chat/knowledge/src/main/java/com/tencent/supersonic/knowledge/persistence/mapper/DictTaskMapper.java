package com.tencent.supersonic.knowledge.persistence.mapper;

import com.tencent.supersonic.knowledge.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.knowledge.dictionary.DictTaskFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictTaskMapper {

    Long createDimValueTask(DictTaskDO dictTaskDO);

    Boolean updateTaskStatus(DictTaskDO dictTaskDO);

    List<DictTaskDO> searchDictTaskList(DictTaskFilter filter);
}
