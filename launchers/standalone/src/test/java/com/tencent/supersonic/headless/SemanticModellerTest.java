package com.tencent.supersonic.headless;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.util.LLMConfigUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.sql.SQLException;
import java.util.Map;

@Disabled
@TestPropertySource(properties = {"s2.model.building.exemplars.enabled = false"})
public class SemanticModellerTest extends BaseTest {

    private LLMConfigUtils.LLMType llmType = LLMConfigUtils.LLMType.OLLAMA_LLAMA3;

    @Autowired
    private ModelService modelService;

    @Test
    public void testBuildModelBatch() throws SQLException {
        ChatModelConfig llmConfig = LLMConfigUtils.getLLMConfig(llmType);
        ModelBuildReq modelSchemaReq = new ModelBuildReq();
        modelSchemaReq.setChatModelConfig(llmConfig);
        modelSchemaReq.setBuildByLLM(true);
        modelSchemaReq.setDatabaseId(1L);
        modelSchemaReq.setDb("semantic");
        modelSchemaReq.setTables(Lists.newArrayList("s2_user_department", "s2_stay_time_statis"));
        Map<String, ModelSchema> modelSchemaMap = modelService.buildModelSchema(modelSchemaReq);

        ModelSchema userModelSchema = modelSchemaMap.get("s2_user_department");
        Assertions.assertEquals(2, userModelSchema.getColumnSchemas().size());
        Assertions.assertEquals(FieldType.primary_key,
                userModelSchema.getColumnByName("user_name").getFiledType());
        Assertions.assertEquals(FieldType.categorical,
                userModelSchema.getColumnByName("department").getFiledType());

        ModelSchema stayTimeModelSchema = modelSchemaMap.get("s2_stay_time_statis");
        Assertions.assertEquals(4, stayTimeModelSchema.getColumnSchemas().size());
        Assertions.assertEquals(FieldType.foreign_key,
                stayTimeModelSchema.getColumnByName("user_name").getFiledType());
        Assertions.assertEquals(FieldType.partition_time,
                stayTimeModelSchema.getColumnByName("imp_date").getFiledType());
        Assertions.assertEquals(FieldType.categorical,
                stayTimeModelSchema.getColumnByName("page").getFiledType());
        Assertions.assertEquals(FieldType.measure,
                stayTimeModelSchema.getColumnByName("stay_hours").getFiledType());
        Assertions.assertEquals(AggOperatorEnum.SUM,
                stayTimeModelSchema.getColumnByName("stay_hours").getAgg());
    }

    @Test
    public void testBuildModelBySql() throws SQLException {
        ChatModelConfig llmConfig = LLMConfigUtils.getLLMConfig(llmType);
        ModelBuildReq modelSchemaReq = new ModelBuildReq();
        modelSchemaReq.setChatModelConfig(llmConfig);
        modelSchemaReq.setBuildByLLM(true);
        modelSchemaReq.setDatabaseId(1L);
        modelSchemaReq.setDb("semantic");
        modelSchemaReq.setSql(
                "SELECT imp_date, user_name, page, 1 as pv, user_name as uv FROM s2_pv_uv_statis");
        Map<String, ModelSchema> modelSchemaMap = modelService.buildModelSchema(modelSchemaReq);

        ModelSchema pvModelSchema = modelSchemaMap.values().iterator().next();
        Assertions.assertEquals(5, pvModelSchema.getColumnSchemas().size());
        Assertions.assertEquals(FieldType.partition_time,
                pvModelSchema.getColumnByName("imp_date").getFiledType());
        Assertions.assertEquals(FieldType.categorical,
                pvModelSchema.getColumnByName("user_name").getFiledType());
        Assertions.assertEquals(FieldType.categorical,
                pvModelSchema.getColumnByName("page").getFiledType());
        Assertions.assertEquals(FieldType.measure,
                pvModelSchema.getColumnByName("pv").getFiledType());
        Assertions.assertEquals(AggOperatorEnum.SUM, pvModelSchema.getColumnByName("pv").getAgg());
        Assertions.assertEquals(FieldType.measure,
                pvModelSchema.getColumnByName("uv").getFiledType());
        Assertions.assertEquals(AggOperatorEnum.COUNT_DISTINCT,
                pvModelSchema.getColumnByName("uv").getAgg());
    }

}
