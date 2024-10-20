package com.tencent.supersonic.chat.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryUpdateReq;
import com.tencent.supersonic.chat.api.pojo.request.PageMemoryReq;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;

public interface MemoryService {
    void createMemory(ChatMemoryDO memory);

    void updateMemory(ChatMemoryUpdateReq chatMemoryUpdateReq, User user);

    void updateMemory(ChatMemoryDO memory);

    void enableMemory(ChatMemoryDO memory);

    void disableMemory(ChatMemoryDO memory);

    void batchDelete(List<Long> ids);

    PageInfo<ChatMemoryDO> pageMemories(PageMemoryReq pageMemoryReq);

    List<ChatMemoryDO> getMemories(ChatMemoryFilter chatMemoryFilter);

    List<ChatMemoryDO> getMemoriesForLlmReview();
}
