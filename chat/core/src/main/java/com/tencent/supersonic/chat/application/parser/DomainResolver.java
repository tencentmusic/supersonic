package com.tencent.supersonic.chat.application.parser;


import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.component.SemanticQuery;

import java.util.Map;

public interface DomainResolver {

    Integer resolve(Map<Integer, SemanticQuery> domainQueryModes, QueryContextReq queryCtx, ChatContext chatCtx,
            SchemaMapInfo schemaMap);

    boolean isDomainSwitch(ChatContext chatCtx, SemanticParseInfo semanticParseInfo);

}