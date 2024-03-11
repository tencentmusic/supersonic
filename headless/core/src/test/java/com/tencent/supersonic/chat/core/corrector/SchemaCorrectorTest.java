package com.tencent.supersonic.chat.core.corrector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.core.chat.corrector.SchemaCorrector;
import com.tencent.supersonic.headless.core.chat.parser.llm.ParseResult;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

class SchemaCorrectorTest {

    private String json = "{\n"
            + "          \"dataSetId\":  1,\n"
            + "          \"llmReq\":  {\n"
            + "                    \"queryText\":  \"xxx2024年播放量最高的十首歌\",\n"
            + "                    \"filterCondition\":  {\n"
            + "                              \"tableName\":  null\n"
            + "                    },\n"
            + "                    \"schema\":  {\n"
            + "                              \"domainName\":  \"歌曲\",\n"
            + "                              \"dataSetName\":  \"歌曲\",\n"
            + "                              \"fieldNameList\":  [\n"
            + "                                        \"商务组\",\n"
            + "                                        \"歌曲名\",\n"
            + "                                        \"播放量\",\n"
            + "                                        \"播放份额\",\n"
            + "                                        \"数据日期\"\n"
            + "                              ]\n"
            + "                    },\n"
            + "                    \"linking\":  [\n"
            + "\n"
            + "                    ],\n"
            + "                    \"currentDate\":  \"2024-02-24\",\n"
            + "                    \"priorExts\":  \"播放份额是小数; \",\n"
            + "                    \"sqlGenerationMode\":  \"2_pass_auto_cot\"\n"
            + "          },\n"
            + "          \"request\":  null,\n"
            + "          \"commonAgentTool\":  {\n"
            + "                    \"id\":  \"y3LqVSRL\",\n"
            + "                    \"name\":  \"大模型语义解析\",\n"
            + "                    \"type\":  \"NL2SQL_LLM\",\n"
            + "                    \"dataSetIds\":  [\n"
            + "                              1\n"
            + "                    ]\n"
            + "          },\n"
            + "          \"linkingValues\":  [\n"
            + "\n"
            + "          ]\n"
            + "}";

    @Test
    void doCorrect() throws JsonProcessingException {
        Long dataSetId = 1L;
        QueryContext queryContext = buildQueryContext(dataSetId);
        ObjectMapper objectMapper = new ObjectMapper();
        ParseResult parseResult = objectMapper.readValue(json, ParseResult.class);


        String sql = "select  歌曲名 from 歌曲 where 发行日期 >= '2024-01-01' "
                + "and 商务组 = 'xxx' order by 播放量 desc  limit 10";
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SqlInfo sqlInfo = new SqlInfo();
        sqlInfo.setS2SQL(sql);
        sqlInfo.setCorrectS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);

        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSet(dataSetId);
        semanticParseInfo.setDataSet(schemaElement);


        semanticParseInfo.getProperties().put(Constants.CONTEXT, parseResult);

        SchemaCorrector schemaCorrector = new SchemaCorrector();
        schemaCorrector.removeFilterIfNotInLinkingValue(queryContext, semanticParseInfo);

        assertEquals("SELECT 歌曲名 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' "
                + "ORDER BY 播放量 DESC LIMIT 10", semanticParseInfo.getSqlInfo().getCorrectS2SQL());

        parseResult = objectMapper.readValue(json, ParseResult.class);

        List<LLMReq.ElementValue> linkingValues = new ArrayList<>();
        LLMReq.ElementValue elementValue = new LLMReq.ElementValue();
        elementValue.setFieldName("商务组");
        elementValue.setFieldValue("xxx");
        linkingValues.add(elementValue);
        parseResult.setLinkingValues(linkingValues);
        semanticParseInfo.getProperties().put(Constants.CONTEXT, parseResult);

        semanticParseInfo.getSqlInfo().setCorrectS2SQL(sql);
        semanticParseInfo.getSqlInfo().setS2SQL(sql);
        schemaCorrector.removeFilterIfNotInLinkingValue(queryContext, semanticParseInfo);
        assertEquals("SELECT 歌曲名 FROM 歌曲 WHERE 发行日期 >= '2024-01-01' "
                + "AND 商务组 = 'xxx' ORDER BY 播放量 DESC LIMIT 10", semanticParseInfo.getSqlInfo().getCorrectS2SQL());

    }

    private QueryContext buildQueryContext(Long dataSetId) {
        QueryContext queryContext = new QueryContext();
        List<DataSetSchema> dataSetSchemaList = new ArrayList<>();
        DataSetSchema dataSetSchema = new DataSetSchema();
        QueryConfig queryConfig = new QueryConfig();
        dataSetSchema.setQueryConfig(queryConfig);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSet(dataSetId);
        dataSetSchema.setDataSet(schemaElement);
        Set<SchemaElement> dimensions = new HashSet<>();
        SchemaElement element1 = new SchemaElement();
        element1.setDataSet(1L);
        element1.setName("歌曲名");
        dimensions.add(element1);

        SchemaElement element2 = new SchemaElement();
        element2.setDataSet(1L);
        element2.setName("商务组");
        dimensions.add(element2);

        SchemaElement element3 = new SchemaElement();
        element3.setDataSet(1L);
        element3.setName("发行日期");
        dimensions.add(element3);

        dataSetSchema.setDimensions(dimensions);
        dataSetSchemaList.add(dataSetSchema);

        SemanticSchema semanticSchema = new SemanticSchema(dataSetSchemaList);
        queryContext.setSemanticSchema(semanticSchema);
        return queryContext;
    }
}