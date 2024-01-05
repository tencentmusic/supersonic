package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.server.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.server.persistence.repository.StatisticsRepository;
import com.tencent.supersonic.chat.server.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Async
    @Override
    public void batchSaveStatistics(List<StatisticsDO> list) {
        statisticsRepository.batchSaveStatistics(list);
    }
}
