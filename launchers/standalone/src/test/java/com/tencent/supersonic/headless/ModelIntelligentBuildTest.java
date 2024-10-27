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
public class ModelIntelligentBuildTest extends BaseTest {

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
        Assertions.assertEquals(2, userModelSchema.getFiledSchemas().size());
        Assertions.assertEquals(FieldType.primary_key,
                userModelSchema.getFieldByName("user_name").getFiledType());
        Assertions.assertEquals(FieldType.dimension,
                userModelSchema.getFieldByName("department").getFiledType());

        ModelSchema stayTimeModelSchema = modelSchemaMap.get("s2_stay_time_statis");
        Assertions.assertEquals(4, stayTimeModelSchema.getFiledSchemas().size());
        Assertions.assertEquals(FieldType.foreign_key,
                stayTimeModelSchema.getFieldByName("user_name").getFiledType());
        Assertions.assertEquals(FieldType.data_time,
                stayTimeModelSchema.getFieldByName("imp_date").getFiledType());
        Assertions.assertEquals(FieldType.dimension,
                stayTimeModelSchema.getFieldByName("page").getFiledType());
        Assertions.assertEquals(FieldType.measure,
                stayTimeModelSchema.getFieldByName("stay_hours").getFiledType());
        Assertions.assertEquals(AggOperatorEnum.SUM,
                stayTimeModelSchema.getFieldByName("stay_hours").getAgg());
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
        Assertions.assertEquals(5, pvModelSchema.getFiledSchemas().size());
        Assertions.assertEquals(FieldType.data_time,
                pvModelSchema.getFieldByName("imp_date").getFiledType());
        Assertions.assertEquals(FieldType.dimension,
                pvModelSchema.getFieldByName("user_name").getFiledType());
        Assertions.assertEquals(FieldType.dimension,
                pvModelSchema.getFieldByName("page").getFiledType());
        Assertions.assertEquals(FieldType.measure,
                pvModelSchema.getFieldByName("pv").getFiledType());
        Assertions.assertEquals(AggOperatorEnum.SUM, pvModelSchema.getFieldByName("pv").getAgg());
        Assertions.assertEquals(FieldType.measure,
                pvModelSchema.getFieldByName("uv").getFiledType());
        Assertions.assertEquals(AggOperatorEnum.COUNT_DISTINCT,
                pvModelSchema.getFieldByName("uv").getAgg());
    }

}
