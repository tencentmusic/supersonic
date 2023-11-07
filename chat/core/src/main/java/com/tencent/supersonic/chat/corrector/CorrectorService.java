package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;

public interface CorrectorService {

    SemanticCorrectInfo correctorSql(QueryFilters queryFilters, SemanticParseInfo parseInfo, String sql);

    void addS2QLAndLoginSql(QueryStructReq queryStructReq, SemanticParseInfo parseInfo);
}
