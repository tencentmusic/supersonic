package com.tencent.supersonic.headless.chat.corrector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.llm.ParseResult;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Disabled
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
    void doCorrect() throws JsonProcessingException {
        Long dataSetId = 1L;
        ChatQueryContext chatQueryContext = buildQueryContext(dataSetId);
        ObjectMapper objectMapper = new ObjectMapper();
        ParseResult parseResult = objectMapper.readValue(json, ParseResult.class);

        String sql = "select  歌曲名 from 歌曲 where 发行日期 >= '2024-01-01' "
                + "and 商务组 = 'xxx' order by 播放量 desc  limit 10";
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SqlInfo sqlInfo = new SqlInfo();
        sqlInfo.setParsedS2SQL(sql);
        sqlInfo.setCorrectedS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);

        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSetId(dataSetId);
        semanticParseInfo.setDataSet(schemaElement);

        semanticParseInfo.getProperties().put(Constants.CONTEXT, parseResult);

        SchemaCorrector schemaCorrector = new SchemaCorrector();
        schemaCorrector.removeFilterIfNotInLinkingValue(chatQueryContext, semanticParseInfo);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' " + "ORDER BY 播放量 DESC LIMIT 10",
                semanticParseInfo.getSqlInfo().getCorrectedS2SQL());

        parseResult = objectMapper.readValue(json, ParseResult.class);

        List<LLMReq.ElementValue> linkingValues = new ArrayList<>();
        LLMReq.ElementValue elementValue = new LLMReq.ElementValue();
        elementValue.setFieldName("商务组");
        elementValue.setFieldValue("xxx");
        linkingValues.add(elementValue);
        semanticParseInfo.getProperties().put(Constants.CONTEXT, parseResult);

        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(sql);
        semanticParseInfo.getSqlInfo().setParsedS2SQL(sql);
        schemaCorrector.removeFilterIfNotInLinkingValue(chatQueryContext, semanticParseInfo);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' "
                        + "AND 商务组 = 'xxx' ORDER BY 播放量 DESC LIMIT 10",
                semanticParseInfo.getSqlInfo().getCorrectedS2SQL());
    }

    private ChatQueryContext buildQueryContext(Long dataSetId) {
        ChatQueryContext chatQueryContext = new ChatQueryContext();
        List<DataSetSchema> dataSetSchemaList = new ArrayList<>();
        DataSetSchema dataSetSchema = new DataSetSchema();
        QueryConfig queryConfig = new QueryConfig();
        dataSetSchema.setQueryConfig(queryConfig);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSetId(dataSetId);
        dataSetSchema.setDataSet(schemaElement);
        Set<SchemaElement> dimensions = new HashSet<>();
        SchemaElement element1 = new SchemaElement();
        element1.setDataSetId(1L);
        element1.setName("歌曲名");
        dimensions.add(element1);

        SchemaElement element2 = new SchemaElement();
        element2.setDataSetId(1L);
        element2.setName("商务组");
        dimensions.add(element2);

        SchemaElement element3 = new SchemaElement();
        element3.setDataSetId(1L);
        element3.setName("发行日期");
        dimensions.add(element3);

        dataSetSchema.setDimensions(dimensions);
        dataSetSchemaList.add(dataSetSchema);

        SemanticSchema semanticSchema = new SemanticSchema(dataSetSchemaList);
        chatQueryContext.setSemanticSchema(semanticSchema);
        return chatQueryContext;
    }
}
