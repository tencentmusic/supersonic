package com.tencent.supersonic.headless.server.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class ModelIntelligentBuilder extends IntelligentBuilder {

    public static final String APP_KEY = "BUILD_DATA_MODEL";

    private static final String SYS_EXEMPLAR_FILE = "s2-buildModel-exemplar.json";

    public static final String INSTRUCTION = ""
            + "Role: As an experienced data analyst with extensive modeling experience, "
            + "      you are expected to have a deep understanding of data analysis and data modeling concepts."
            + "\nJob: You will be given a database table structure, which includes the database table name, field name,"
            + "       field type, and field comments. Your task is to utilize this information for data modeling."
            + "\nTask:"
            + "\n1. Generate a name and description for the model. Please note, 'bizName' refers to the English name, while 'name' is the Chinese name."
            + "\n2. Create a Chinese name for the field and categorize the field into one of the following five types:"
            + "\n   primary_key: This is a unique identifier for a record row in a database."
            + "\n   foreign_key: This is a key in a database whose value is derived from the primary key of another table."
            + "\n   data_time: This represents the time when data is generated in the data warehouse."
            + "\n   dimension: Usually a string type, used for grouping and filtering data. No need to generate aggregate functions"
            + "\n   measure: Usually a numeric type, used to quantify data from a certain evaluative perspective. "
            + "              Also, you need to generate aggregate functions(Eg: MAX, MIN, AVG, SUM, COUNT) for the measure type. "
            + "\nTip: I will also give you other related dbSchemas. If you determine that different dbSchemas have the same fields, "
            + "       they can be primary and foreign key relationships."
            + "\nDBSchema: {{DBSchema}}" + "\nOtherRelatedDBSchema: {{otherRelatedDBSchema}}"
            + "\nExemplar: {{exemplar}}";

    private final ObjectMapper objectMapper = JsonUtil.INSTANCE.getObjectMapper();

    @Value("${s2.model.building.exemplars.enabled:true}")
    private Boolean enableExemplarLoading;

    public ModelIntelligentBuilder() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("构造数据语义模型")
                .appModule(AppModule.HEADLESS).description("通过大模型来构造数据语义模型").enable(true).build());
    }

    interface ModelSchemaExtractor {
        ModelSchema generateModelSchema(String text);
    }


    public ModelSchema build(DbSchema dbSchema, List<DbSchema> otherDbSchema,
            ModelBuildReq modelBuildReq) {
        Optional<ChatApp> chatApp = ChatAppManager.getApp(APP_KEY);
        if (!chatApp.isPresent() || !chatApp.get().isEnable()) {
            return null;
        }
        ChatModelConfig chatModelConfig = modelBuildReq.getChatModelConfig();
        ModelSchemaExtractor extractor =
                AiServices.create(ModelSchemaExtractor.class, getChatModel(chatModelConfig));
        Prompt prompt = generatePrompt(dbSchema, otherDbSchema, chatApp.get());
        ModelSchema modelSchema =
                extractor.generateModelSchema(prompt.toUserMessage().singleText());
        log.info("dbSchema:  {}\n otherRelatedDBSchema:{}\n modelSchema: {}",
                JsonUtil.toString(dbSchema), JsonUtil.toString(otherDbSchema),
                JsonUtil.toString(modelSchema));
        return modelSchema;
    }

    private Prompt generatePrompt(DbSchema dbSchema, List<DbSchema> otherDbSchema,
            ChatApp chatApp) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("exemplar", loadExemplars());
        variable.put("DBSchema", JsonUtil.toString(dbSchema));
        variable.put("otherRelatedDBSchema", JsonUtil.toString(otherDbSchema));
        return PromptTemplate.from(chatApp.getPrompt()).apply(variable);
    }

    private String loadExemplars() {
        if (!enableExemplarLoading) {
            log.info("Not enable load model-building exemplars");
            return "";
        }
        try {
            ClassPathResource resource = new ClassPathResource(SYS_EXEMPLAR_FILE);
            if (resource.exists()) {
                InputStream inputStream = resource.getInputStream();
                return objectMapper
                        .writeValueAsString(objectMapper.readValue(inputStream, Object.class));
            }
        } catch (Exception e) {
            log.error("Failed to load model-building system exemplars", e);
        }
        return "";
    }

}
