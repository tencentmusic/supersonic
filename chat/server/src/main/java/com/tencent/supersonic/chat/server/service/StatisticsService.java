package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.server.persistence.dataobject.StatisticsDO;

import java.util.List;

public interface StatisticsService {
    void batchSaveStatistics(List<StatisticsDO> list);
}
