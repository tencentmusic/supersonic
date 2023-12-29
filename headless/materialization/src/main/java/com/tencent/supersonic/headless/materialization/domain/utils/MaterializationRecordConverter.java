package com.tencent.supersonic.headless.materialization.domain.utils;

import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.common.materialization.request.MaterializationRecordReq;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationRecordResp;
import com.tencent.supersonic.headless.materialization.domain.pojo.MaterializationRecord;
import com.tencent.supersonic.headless.materialization.domain.dataobject.MaterializationRecordDO;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;

import java.util.Objects;

public class MaterializationRecordConverter {
    public static MaterializationRecord req2Bean(MaterializationRecordReq materializationRecordReq) {
        MaterializationRecord materializationRecord = new MaterializationRecord();
        BeanUtils.copyProperties(materializationRecordReq, materializationRecord);
        return materializationRecord;
    }

    public static MaterializationRecordDO materializationRecord2DO(MaterializationRecord materializationRecord) {
        MaterializationRecordDO materializationRecordDO = new MaterializationRecordDO();
        BeanUtils.copyProperties(materializationRecord, materializationRecordDO);
        if (Objects.nonNull(materializationRecord.getElementType())) {
            materializationRecordDO.setElementType(materializationRecord.getElementType().name());
        }
        if (Objects.nonNull(materializationRecord.getTaskStatus())) {
            materializationRecordDO.setState(materializationRecord.getTaskStatus().name());
        }
        return materializationRecordDO;
    }

    public static MaterializationRecordDO convert(MaterializationRecordDO materializationRecordDO,
                                                  MaterializationRecord materializationRecord) {
        BeanMapper.mapper(materializationRecord, materializationRecordDO);
        if (Objects.nonNull(materializationRecord.getElementType())) {
            materializationRecordDO.setElementType(materializationRecord.getElementType().name());
        }
        if (Objects.nonNull(materializationRecord.getTaskStatus())) {
            materializationRecordDO.setState(materializationRecord.getTaskStatus().name());
        }
        return materializationRecordDO;
    }

    public static MaterializationRecordResp materializationRecordDO2Resp(MaterializationRecordDO recordDO) {
        MaterializationRecordResp materializationRecordResp = new MaterializationRecordResp();
        BeanUtils.copyProperties(recordDO, materializationRecordResp);
        if (Strings.isNotEmpty(recordDO.getElementType())) {
            materializationRecordResp.setElementType(TypeEnums.of(recordDO.getElementType()));
        }
        if (Strings.isNotEmpty(recordDO.getState())) {
            materializationRecordResp.setTaskStatus(TaskStatusEnum.of(recordDO.getState()));
        }
        return materializationRecordResp;
    }
}