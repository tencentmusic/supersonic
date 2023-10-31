package com.tencent.supersonic.semantic.materialization.domain.utils;

import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;

public interface MaterializationUtils {

    String generateCreateSql(MaterializationResp materializationResp);
}
