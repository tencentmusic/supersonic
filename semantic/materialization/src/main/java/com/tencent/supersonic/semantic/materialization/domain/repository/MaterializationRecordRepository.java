package com.tencent.supersonic.semantic.materialization.domain.repository;

import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationRecordFilter;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationRecordResp;
import com.tencent.supersonic.semantic.materialization.domain.pojo.MaterializationRecord;

import java.util.List;

public interface MaterializationRecordRepository {

    Boolean insertMaterializationRecord(MaterializationRecord materializationRecord);

    Boolean updateMaterializationRecord(MaterializationRecord materializationRecord);

    List<MaterializationRecordResp> getMaterializationRecordList(MaterializationRecordFilter filter);

    long getCount(MaterializationRecordFilter filter);
}
