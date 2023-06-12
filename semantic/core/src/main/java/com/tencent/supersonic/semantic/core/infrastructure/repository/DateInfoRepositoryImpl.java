package com.tencent.supersonic.semantic.core.infrastructure.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.tencent.supersonic.semantic.api.core.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.core.request.DateInfoReq;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.semantic.core.domain.dataobject.DateInfoDO;
import com.tencent.supersonic.semantic.core.domain.repository.DateInfoRepository;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.DateInfoMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Slf4j
@Repository
public class DateInfoRepositoryImpl implements DateInfoRepository {

//    @Autowired
//    private SqlSessionTemplate sqlSessionTemplate;

    private SqlSession sqlSession;

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private DateInfoMapper dateInfoMapper;

//    @PostConstruct
//    public void init() {
//        if (Objects.isNull(sqlSession)) {
//            sqlSession = sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false);
//        }
//    }

    @PreDestroy
    public void preDestroy() {
        if (Objects.nonNull(sqlSession)) {
            sqlSession.close();
        }
    }

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
        try {
            sqlSession.commit();
            return dateInfoDOList.size();
        } catch (Exception e) {
            log.warn("e:", e);
        }
        log.info("before final, elapsed time:{}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return 0;
    }
}