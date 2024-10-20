package com.tencent.supersonic.headless.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.QueryDataType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.enums.ChatWorkflowState;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.chat.parser.ParserConfig;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueryContext {

    private String queryText;
    private String oriQueryText;
    private Set<Long> dataSetIds;
    private Map<Long, List<Long>> modelIdToDataSetIds;
    private User user;
    private boolean saveAnswer;
    @Builder.Default
    private Text2SQLType text2SQLType = Text2SQLType.RULE_AND_LLM;
    private QueryFilters queryFilters;
    private List<SemanticQuery> candidateQueries = new ArrayList<>();
    private SchemaMapInfo mapInfo = new SchemaMapInfo();
    private SemanticParseInfo contextParseInfo;
    private MapModeEnum mapModeEnum = MapModeEnum.STRICT;
    private QueryDataType queryDataType = QueryDataType.ALL;
    @JsonIgnore
    private SemanticSchema semanticSchema;
    @JsonIgnore
    private ChatWorkflowState chatWorkflowState;
    @JsonIgnore
    private Map<String, ChatApp> chatAppConfig;
    @JsonIgnore
    private List<Text2SQLExemplar> dynamicExemplars;

    public List<SemanticQuery> getCandidateQueries() {
        ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
        int parseShowCount =
                Integer.parseInt(parserConfig.getParameterValue(ParserConfig.PARSER_SHOW_COUNT));
        candidateQueries = candidateQueries.stream()
                .sorted(Comparator.comparing(
                        semanticQuery -> semanticQuery.getParseInfo().getScore(),
                        Comparator.reverseOrder()))
                .limit(parseShowCount).collect(Collectors.toList());
        return candidateQueries;
    }

    public boolean containsPartitionDimensions(Long dataSetId) {
        SemanticSchema semanticSchema = this.getSemanticSchema();
        DataSetSchema dataSetSchema = semanticSchema.getDataSetSchemaMap().get(dataSetId);
        return dataSetSchema.containsPartitionDimensions();
    }
}
