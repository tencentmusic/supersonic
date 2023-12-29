package com.tencent.supersonic.headless.materialization.domain.utils;

import com.tencent.supersonic.headless.common.materialization.response.MaterializationResp;

public interface MaterializationUtils {

    String generateCreateSql(MaterializationResp materializationResp);
}
