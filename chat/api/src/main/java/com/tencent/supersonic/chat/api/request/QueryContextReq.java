package com.tencent.supersonic.chat.api.request;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QueryContextReq {

    private String queryText;
    private Integer chatId;
    private Integer domainId = 0;
    private User user;
    private QueryFilter queryFilter;
    private List<SemanticQuery> candidateQueries = new ArrayList<>();
    private SchemaMapInfo mapInfo = new SchemaMapInfo();
    private boolean saveAnswer = true;
}
