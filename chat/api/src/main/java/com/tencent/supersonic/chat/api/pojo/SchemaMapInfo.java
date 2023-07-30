package com.tencent.supersonic.chat.api.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchemaMapInfo {

    private Map<Long, List<SchemaElementMatch>> domainElementMatches = new HashMap<>();

    public Set<Long> getMatchedDomains() {
        return domainElementMatches.keySet();
    }

    public List<SchemaElementMatch> getMatchedElements(Long domain) {
        return domainElementMatches.get(domain);
    }

    public Map<Long, List<SchemaElementMatch>> getDomainElementMatches() {
        return domainElementMatches;
    }

    public void setMatchedElements(Long domain, List<SchemaElementMatch> elementMatches) {
        domainElementMatches.put(domain, elementMatches);
    }
}
