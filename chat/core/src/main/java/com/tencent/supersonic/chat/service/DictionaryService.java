package com.tencent.supersonic.chat.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.knowledge.dictionary.DictConfig;
import com.tencent.supersonic.knowledge.dictionary.DictTaskFilter;
import com.tencent.supersonic.knowledge.dictionary.DimValue2DictCommand;
import com.tencent.supersonic.knowledge.dictionary.DimValueDictInfo;

import java.util.List;

public interface DictionaryService {
    Long addDictTask(DimValue2DictCommand dimValue2DictCommend, User user);

    Long deleteDictTask(DimValue2DictCommand dimValue2DictCommend, User user);

    List<DimValueDictInfo> searchDictTaskList(DictTaskFilter filter, User user);

    DictConfig getDictInfoByModelId(Long modelId);

    String getDictRootPath();
}
