package com.tencent.supersonic.semantic.materialization.domain.utils;

import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationRecordReq;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationRecordResp;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationRecordDO;
import com.tencent.supersonic.semantic.materialization.domain.pojo.MaterializationRecord;
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