package com.tencent.supersonic.chat.application.parser.resolver;


import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticQuery;
import java.util.Map;

public interface DomainResolver {

    Integer resolve(Map<Integer, SemanticQuery> domainQueryModes, QueryContextReq queryCtx, ChatContext chatCtx,
            SchemaMapInfo schemaMap);

    boolean isDomainSwitch(ChatContext chatCtx, QueryContextReq queryCtx);

}