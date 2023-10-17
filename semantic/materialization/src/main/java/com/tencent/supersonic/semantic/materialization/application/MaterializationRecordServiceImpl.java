package com.tencent.supersonic.semantic.materialization.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationDateFilter;
import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationRecordFilter;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationRecordReq;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationDateResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationRecordResp;
import com.tencent.supersonic.semantic.materialization.domain.MaterializationRecordService;
import com.tencent.supersonic.semantic.materialization.domain.pojo.MaterializationRecord;
import com.tencent.supersonic.semantic.materialization.domain.repository.MaterializationRecordRepository;
import com.tencent.supersonic.semantic.materialization.domain.utils.MaterializationRecordConverter;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MaterializationRecordServiceImpl implements MaterializationRecordService {

    private final MaterializationRecordRepository repository;

    public MaterializationRecordServiceImpl(MaterializationRecordRepository materializationRecordRepository) {
        this.repository = materializationRecordRepository;
    }

    @Override
    public Boolean addMaterializationRecord(MaterializationRecordReq materializationRecordReq, User user) {
        log.info("materializationRecordReq:{}, user:{}", JsonUtil.toString(materializationRecordReq),
                JsonUtil.toString(user));
        MaterializationRecord materializationRecord = MaterializationRecordConverter.req2Bean(materializationRecordReq);
        RecordInfo recordInfo = new RecordInfo().createdBy(user.getName());
        BeanUtils.copyProperties(recordInfo, materializationRecord);
        return repository.insertMaterializationRecord(materializationRecord);
    }

    @Override
    public Boolean updateMaterializationRecord(MaterializationRecordReq materializationRecordReq, User user) {
        log.info("materializationRecordReq:{}, user:{}", JsonUtil.toString(materializationRecordReq),
                JsonUtil.toString(user));
        MaterializationRecord materializationRecord = MaterializationRecordConverter.req2Bean(materializationRecordReq);
        RecordInfo recordInfo = new RecordInfo().updatedBy(user.getName());
        BeanUtils.copyProperties(recordInfo, materializationRecord);
        return repository.updateMaterializationRecord(materializationRecord);
    }

    @Override
    public List<MaterializationRecordResp> getMaterializationRecordList(MaterializationRecordFilter filter, User user) {
        return repository.getMaterializationRecordList(filter);
    }

    @Override
    public Long getMaterializationRecordCount(MaterializationRecordFilter filter, User user) {
        return repository.getCount(filter);
    }

    @Override
    public List<MaterializationDateResp> fetchMaterializationDate(MaterializationDateFilter filter, User user) {
        return null;
    }

    @Override
    public List<MaterializationRecordResp> fetchMaterializationDate(List<Long> materializationIds, String elementName,
            String startTime, String endTime) {
        MaterializationRecordFilter materializationRecordFilter = MaterializationRecordFilter.builder()
                .taskStatus(Arrays.asList(TaskStatusEnum.SUCCESS))
                .elementName(elementName)
                .materializationIds(materializationIds)
                .startDataTime(startTime)
                .endDataTime(endTime).build();
        return repository.getMaterializationRecordList(materializationRecordFilter);
    }

}