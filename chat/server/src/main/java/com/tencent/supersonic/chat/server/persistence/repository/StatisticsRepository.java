package com.tencent.supersonic.chat.server.persistence.repository;


import com.tencent.supersonic.chat.server.persistence.dataobject.StatisticsDO;

import java.util.List;

public interface StatisticsRepository {

    void batchSaveStatistics(List<StatisticsDO> list);
}
