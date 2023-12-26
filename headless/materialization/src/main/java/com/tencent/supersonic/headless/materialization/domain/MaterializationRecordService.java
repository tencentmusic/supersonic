package com.tencent.supersonic.headless.materialization.domain;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationDateFilter;
import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationRecordFilter;
import com.tencent.supersonic.headless.common.materialization.request.MaterializationRecordReq;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationDateResp;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationRecordResp;
import java.util.List;

public interface MaterializationRecordService {

    Boolean addMaterializationRecord(MaterializationRecordReq materializationRecord, User user);

    Boolean updateMaterializationRecord(MaterializationRecordReq materializationRecord, User user);

    List<MaterializationRecordResp> getMaterializationRecordList(MaterializationRecordFilter filter, User user);

    Long getMaterializationRecordCount(MaterializationRecordFilter filter, User user);

    List<MaterializationDateResp> fetchMaterializationDate(MaterializationDateFilter filter, User user);

    List<MaterializationRecordResp> fetchMaterializationDate(List<Long> materializationIds, String elementName,
            String startTime, String endTime);

}
