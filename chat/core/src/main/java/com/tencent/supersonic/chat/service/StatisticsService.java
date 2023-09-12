package com.tencent.supersonic.chat.service;

import com.tencent.supersonic.chat.persistence.dataobject.StatisticsDO;

import java.util.List;

public interface StatisticsService {
    boolean batchSaveStatistics(List<StatisticsDO> list);
}
