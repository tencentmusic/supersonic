package com.tencent.supersonic.chat.api.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchemaMapInfo {

    private Map<Integer, List<SchemaElementMatch>> domainElementMatches = new HashMap<>();

    public Set<Integer> getMatchedDomains() {
        return domainElementMatches.keySet();
    }

    public List<SchemaElementMatch> getMatchedElements(Integer domain) {
        return domainElementMatches.get(domain);
    }

    public Map<Integer, List<SchemaElementMatch>> getDomainElementMatches() {
        return domainElementMatches;
    }

    public void setMatchedElements(Integer domain, List<SchemaElementMatch> elementMatches) {
        domainElementMatches.put(domain, elementMatches);
    }

}
