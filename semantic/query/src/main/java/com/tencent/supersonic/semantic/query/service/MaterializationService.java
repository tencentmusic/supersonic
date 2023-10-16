package com.tencent.supersonic.semantic.query.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationSourceReq;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationSourceResp;

public interface MaterializationService {

    MaterializationSourceResp getMaterializationDataSource(MaterializationSourceReq materializationSourceReq,
            User user) throws Exception;
}
