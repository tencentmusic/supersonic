package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.server.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.server.persistence.mapper.StatisticsMapper;
import com.tencent.supersonic.chat.server.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Async
    @Override
    public void batchSaveStatistics(List<StatisticsDO> list) {
        statisticsMapper.batchSaveStatistics(list);
    }
}
