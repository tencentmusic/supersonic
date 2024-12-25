package com.tencent.supersonic.chat.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryUpdateReq;
import com.tencent.supersonic.chat.api.pojo.request.PageMemoryReq;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;

public interface MemoryService {
    void createMemory(ChatMemory memory);

    void updateMemory(ChatMemoryUpdateReq chatMemoryUpdateReq, User user);

    void batchDelete(List<Long> ids);

    PageInfo<ChatMemory> pageMemories(PageMemoryReq pageMemoryReq);

    List<ChatMemory> getMemories(ChatMemoryFilter chatMemoryFilter);

}
