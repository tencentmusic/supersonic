package com.tencent.supersonic.chat.service.impl;

import com.tencent.supersonic.chat.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.persistence.repository.StatisticsRepository;
import com.tencent.supersonic.chat.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("statisticsService")
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private StatisticsRepository statisticsRepository;

    public StatisticsServiceImpl(StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    @Override
    public boolean batchSaveStatistics(List<StatisticsDO> list) {
        return statisticsRepository.batchSaveStatistics(list);
    }
}
