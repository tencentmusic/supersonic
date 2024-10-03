package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.server.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.server.persistence.mapper.StatisticsMapper;
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
    private StatisticsMapper statisticsMapper;

    @Async
    @Override
    public void batchSaveStatistics(List<StatisticsDO> list) {
        statisticsMapper.batchSaveStatistics(list);
    }
}
