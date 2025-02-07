package com.tencent.supersonic.headless.chat.corrector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.llm.ParseResult;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SchemaCorrectorTest {

    private String json = "{\n" + "          \"dataSetId\":  1,\n" + "          \"llmReq\":  {\n"
            + "                    \"queryText\":  \"xxx2024年播放量最高的十首歌\",\n"
            + "                    \"schema\":  {\n"
            + "                              \"dataSetName\":  \"歌曲\",\n"
            + "                              \"fieldNameList\":  [\n"
            + "                                        \"商务组\",\n"
            + "                                        \"歌曲名\",\n"
            + "                                        \"播放量\",\n"
            + "                                        \"播放份额\",\n"
            + "                                        \"数据日期\"\n"
            + "                              ]\n" + "                    },\n"
            + "                    \"currentDate\":  \"2024-02-24\",\n"
            + "                    \"sqlGenType\":  \"1_pass_self_consistency\"\n"
            + "          },\n" + "          \"request\":  null\n" + "}";

    @Test
    void testCorrectWrongColumnName() {
        String sql = "SELECT 歌曲 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' ORDER BY SUM(播放) DESC LIMIT 10";
        ChatQueryContext chatQueryContext = buildQueryContext(sql);
        SemanticParseInfo parseInfo = chatQueryContext.getCandidateQueries().get(0).getParseInfo();

        SchemaCorrector schemaCorrector = new SchemaCorrector();
        schemaCorrector.correct(chatQueryContext, parseInfo);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' ORDER BY SUM(播放量) DESC LIMIT 10",
                parseInfo.getSqlInfo().getCorrectedS2SQL());
    }

    @Test
    void testRemoveUnmappedFilterValue() throws JsonProcessingException {
        String sql =
                "SELECT 歌曲名 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' AND 商务组 = 'xxx' ORDER BY 播放量 DESC LIMIT 10";
        ChatQueryContext chatQueryContext = buildQueryContext(sql);
        SemanticParseInfo parseInfo = chatQueryContext.getCandidateQueries().get(0).getParseInfo();

        ObjectMapper objectMapper = new ObjectMapper();
        ParseResult parseResult = objectMapper.readValue(json, ParseResult.class);

        parseInfo.getProperties().put(Constants.CONTEXT, parseResult);

        SchemaCorrector schemaCorrector = new SchemaCorrector();
        schemaCorrector.removeUnmappedFilterValue(chatQueryContext, parseInfo);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' ORDER BY 播放量 DESC LIMIT 10",
                parseInfo.getSqlInfo().getCorrectedS2SQL());

        List<LLMReq.ElementValue> linkingValues = new ArrayList<>();
        LLMReq.ElementValue elementValue = new LLMReq.ElementValue();
        elementValue.setFieldName("商务组");
        elementValue.setFieldValue("xxx");
        linkingValues.add(elementValue);
        parseResult.getLlmReq().getSchema().setValues(linkingValues);
        parseInfo.getProperties().put(Constants.CONTEXT, parseResult);

        parseInfo.getSqlInfo().setCorrectedS2SQL(sql);
        parseInfo.getSqlInfo().setParsedS2SQL(sql);
        schemaCorrector.removeUnmappedFilterValue(chatQueryContext, parseInfo);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' "
                        + "AND 商务组 = 'xxx' ORDER BY 播放量 DESC LIMIT 10",
                parseInfo.getSqlInfo().getCorrectedS2SQL());
    }

    private ChatQueryContext buildQueryContext(String sql) {
        Long dataSetId = 1L;

        ChatQueryContext chatQueryContext = new ChatQueryContext();
        List<DataSetSchema> dataSetSchemaList = new ArrayList<>();
        DataSetSchema dataSetSchema = new DataSetSchema();
        QueryConfig queryConfig = new QueryConfig();
        dataSetSchema.setQueryConfig(queryConfig);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSetId(dataSetId);
        dataSetSchema.setDataSet(schemaElement);

        Set<SchemaElement> dimensions = new HashSet<>();
        dimensions.add(SchemaElement.builder().name("歌曲名").dataSetId(dataSetId).build());
        dimensions.add(SchemaElement.builder().name("商务组").dataSetId(dataSetId).build());
        dimensions.add(SchemaElement.builder().name("发行日期").dataSetId(dataSetId).build());
        dimensions.add(SchemaElement.builder().name("播放量").dataSetId(dataSetId).build());
        dataSetSchema.setDimensions(dimensions);
        dataSetSchemaList.add(dataSetSchema);

        SemanticSchema semanticSchema = new SemanticSchema(dataSetSchemaList);
        chatQueryContext.setSemanticSchema(semanticSchema);

        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SqlInfo sqlInfo = new SqlInfo();
        sqlInfo.setParsedS2SQL(sql);
        sqlInfo.setCorrectedS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);
        semanticParseInfo.setDataSet(dataSetSchema.getDataSet());
        LLMSqlQuery sqlQuery = new LLMSqlQuery();
        sqlQuery.setParseInfo(semanticParseInfo);
        chatQueryContext.getCandidateQueries().add(sqlQuery);

        return chatQueryContext;
    }

}
