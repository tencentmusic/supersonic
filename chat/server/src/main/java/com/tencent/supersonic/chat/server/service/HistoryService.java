package com.tencent.supersonic.chat.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.request.*;
import com.tencent.supersonic.chat.server.pojo.ChatHistory;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;

public interface HistoryService {
    void saveHistoryInfo(ParseContext parseContext);

    void createHistory(ChatHistory memory);

    void updateHistory(ChatHistoryUpdateReq chatMemoryUpdateReq, User user);

    void batchDelete(List<Long> ids);

    PageInfo<ChatHistory> pageHistories(PageHistoryReq pageHistoryReq);

    List<ChatHistory> getMemories(ChatHistoryFilter chatHistoryFilter);

    void updateHistoryByQueryId(ChatMemoryUpdateReq chatMemoryUpdateReq, User user);

    void saveHistoryErrorInfo(ParseContext parseContext);
}
