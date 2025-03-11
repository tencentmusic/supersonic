package com.tencent.supersonic.chat.server.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatHistoryDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;

import java.util.List;

public interface ChatHistoryRepository {
    void createHistory(ChatHistoryDO chatHistoryDO);

    void updateHistory(ChatHistoryDO chatHistoryDO);

    void batchDelete(List<Long> ids);

    ChatHistoryDO getHistory(Long id);

    List<ChatHistoryDO> getHistories(QueryWrapper<ChatHistoryDO> queryWrapper);
}
