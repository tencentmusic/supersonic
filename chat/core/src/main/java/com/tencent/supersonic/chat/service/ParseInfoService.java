package com.tencent.supersonic.chat.service;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import java.util.List;

public interface ParseInfoService {

    List<SemanticParseInfo> getTopCandidateParseInfo(List<SemanticParseInfo> selectedParses,
            List<SemanticParseInfo> candidateParses);

    List<SemanticParseInfo> sortParseInfo(List<SemanticQuery> semanticQueries);

    void updateParseInfo(SemanticParseInfo parseInfo);

}
