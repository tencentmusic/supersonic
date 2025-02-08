package com.tencent.supersonic.headless.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.enums.ChatWorkflowState;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class ChatQueryContext implements Serializable {

    private QueryNLReq request;
    private ParseResp parseResp;
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

    public boolean needSQL() {
        return !request.getText2SQLType().equals(Text2SQLType.NONE);
    }

    public DataSetSchema getDataSetSchema(Long dataSetId) {
        return semanticSchema.getDataSetSchema(dataSetId);
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
