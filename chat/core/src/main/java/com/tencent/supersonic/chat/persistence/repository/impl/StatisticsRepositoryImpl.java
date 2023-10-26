package com.tencent.supersonic.chat.persistence.repository.impl;

import com.tencent.supersonic.chat.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.persistence.mapper.StatisticsMapper;
import com.tencent.supersonic.chat.persistence.repository.StatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
@Slf4j
public class StatisticsRepositoryImpl implements StatisticsRepository {

    private final StatisticsMapper statisticsMapper;

    public StatisticsRepositoryImpl(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    public void batchSaveStatistics(List<StatisticsDO> list) {
        statisticsMapper.batchSaveStatistics(list);
    }


}
