package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.request.DateInfoReq;
import com.tencent.supersonic.headless.server.persistence.dataobject.DateInfoDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DateInfoMapper;
import com.tencent.supersonic.headless.server.persistence.repository.DateInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class DateInfoRepositoryImpl implements DateInfoRepository {


    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private DateInfoMapper dateInfoMapper;

    @Override
    public Integer upsertDateInfo(List<DateInfoReq> dateInfoCommends) {
        List<DateInfoDO> dateInfoDOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(dateInfoCommends)) {
            log.info("dateInfoCommends size is 0");
            return 0;
        }

        dateInfoCommends.stream().forEach(commend -> {
            DateInfoDO dateInfoDO = new DateInfoDO();
            BeanUtils.copyProperties(commend, dateInfoDO);
            try {
                dateInfoDO.setUnavailableDateList(mapper.writeValueAsString(commend.getUnavailableDateList()));
                dateInfoDO.setCreatedBy(Constants.ADMIN_LOWER);
                dateInfoDO.setUpdatedBy(Constants.ADMIN_LOWER);
            } catch (JsonProcessingException e) {
                log.info("e,", e);
            }
            dateInfoDOList.add(dateInfoDO);
        });

        return batchUpsert(dateInfoDOList);
    }

    @Override
    public List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter) {
        if (Objects.nonNull(itemDateFilter) && CollectionUtils.isEmpty(itemDateFilter.getItemIds())) {
            return new ArrayList<>();
        }
        return dateInfoMapper.getDateInfos(itemDateFilter);
    }

    private Integer batchUpsert(List<DateInfoDO> dateInfoDOList) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (DateInfoDO dateInfoDO : dateInfoDOList) {
            dateInfoMapper.upsertDateInfo(dateInfoDO);
        }
        log.info("before final, elapsed time:{}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return 0;
    }
}
