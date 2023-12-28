package com.tencent.supersonic.headless.materialization.domain.repository;

import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationRecordFilter;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationRecordResp;
import com.tencent.supersonic.headless.materialization.domain.pojo.MaterializationRecord;

import java.util.List;

public interface MaterializationRecordRepository {

    Boolean insertMaterializationRecord(MaterializationRecord materializationRecord);

    Boolean updateMaterializationRecord(MaterializationRecord materializationRecord);

    List<MaterializationRecordResp> getMaterializationRecordList(MaterializationRecordFilter filter);

    long getCount(MaterializationRecordFilter filter);
}
