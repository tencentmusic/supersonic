package com.tencent.supersonic.chat.core.pojo;

import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaModelClusterMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.core.agent.Agent;
import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.plugin.Plugin;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryContext {

    private QueryReq request;
    private QueryFilters queryFilters;
    private List<SemanticQuery> candidateQueries = new ArrayList<>();
    private SchemaMapInfo mapInfo = new SchemaMapInfo();
    private SchemaModelClusterMapInfo modelClusterMapInfo = new SchemaModelClusterMapInfo();
    private SemanticSchema semanticSchema;
    private Agent agent;
    private Map<Long, ChatConfigRichResp> modelIdToChatRichConfig;
    private Map<String, Plugin> nameToPlugin;
    private List<Plugin> pluginList;

    public List<SemanticQuery> getCandidateQueries() {
        OptimizationConfig optimizationConfig = ContextUtils.getBean(OptimizationConfig.class);
        Integer parseShowCount = optimizationConfig.getParseShowCount();
        candidateQueries = candidateQueries.stream()
                .sorted(Comparator.comparing(semanticQuery -> semanticQuery.getParseInfo().getScore(),
                        Comparator.reverseOrder()))
                .limit(parseShowCount)
                .collect(Collectors.toList());
        return candidateQueries;
    }
}
