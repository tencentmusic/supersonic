package com.tencent.supersonic.headless.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.enums.ChatWorkflowState;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class ChatQueryContext {

    private QueryNLReq request;
    private String oriQueryText;
    private Map<Long, List<Long>> modelIdToDataSetIds;
    private List<SemanticQuery> candidateQueries = new ArrayList<>();
    private SchemaMapInfo mapInfo = new SchemaMapInfo();
    @JsonIgnore
    private SemanticSchema semanticSchema;
    private ChatWorkflowState chatWorkflowState;

    public ChatQueryContext() {
        this(new QueryNLReq());
    }

    public ChatQueryContext(QueryNLReq request) {
        this.request = request;
        SemanticParseInfo parseInfo = request.getSelectedParseInfo();
        if (Objects.nonNull(parseInfo) && Objects.nonNull(parseInfo.getDataSetId())) {
            mapInfo.setMatchedElements(parseInfo.getDataSetId(), parseInfo.getElementMatches());
        }
    }

    public List<SemanticQuery> getCandidateQueries() {
        candidateQueries = candidateQueries.stream()
                .sorted(Comparator.comparing(
                        semanticQuery -> semanticQuery.getParseInfo().getScore(),
                        Comparator.reverseOrder()))
                .limit(1).collect(Collectors.toList());
        return candidateQueries;
    }

    public boolean containsPartitionDimensions(Long dataSetId) {
        SemanticSchema semanticSchema = this.getSemanticSchema();
        DataSetSchema dataSetSchema = semanticSchema.getDataSetSchemaMap().get(dataSetId);
        return dataSetSchema.containsPartitionDimensions();
    }
}
