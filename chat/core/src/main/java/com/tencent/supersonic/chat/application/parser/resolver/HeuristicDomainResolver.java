package com.tencent.supersonic.chat.application.parser.resolver;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementCount;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticQuery;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service("DomainResolver")
@Slf4j
public class HeuristicDomainResolver extends BaseDomainResolver {

    @Override
    public Integer selectDomain(Map<Integer, SemanticQuery> domainQueryModes, QueryContextReq searchCtx,
            ChatContext chatCtx,
            SchemaMapInfo schemaMap) {
        // if QueryContext has domainId and in domainQueryModes
        if (domainQueryModes.containsKey(searchCtx.getDomainId())) {
            log.info("selectDomain from QueryContext [{}]", searchCtx.getDomainId());
            return searchCtx.getDomainId();
        }
        // if ChatContext has domainId and in domainQueryModes
        if (chatCtx.getParseInfo().getDomainId() > 0) {
            Integer domainId = Integer.valueOf(chatCtx.getParseInfo().getDomainId().intValue());
            if (!isAllowSwitch(domainQueryModes, schemaMap, chatCtx, searchCtx, domainId)) {
                log.info("selectDomain from ChatContext [{}]", domainId);
                return domainId;
            }
        }
        // default 0
        return 0;
    }
}