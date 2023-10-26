package com.tencent.supersonic.chat.persistence.repository;


import com.tencent.supersonic.chat.persistence.dataobject.StatisticsDO;

import java.util.List;

public interface StatisticsRepository {

    void batchSaveStatistics(List<StatisticsDO> list);
}
