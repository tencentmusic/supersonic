package com.tencent.supersonic.headless.server.builder;

import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.ModelSchemaReq;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ModelIntelligentBuilder extends IntelligentBuilder {

    public static final String APP_KEY = "BUILD_DATA_MODEL";

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
            + "\n   dimension: Usually a string type, used for grouping and filtering data."
            + "\n   measure: Usually a numeric type, used to quantify data from a certain evaluative perspective."

            + "\nDBSchema: {{DBSchema}}" + "\nExemplar: {{exemplar}}";

    public ModelIntelligentBuilder() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("构造数据语义模型")
                .appModule(AppModule.HEADLESS).description("通过大模型来构造数据语义模型").enable(true).build());
    }


    interface ModelSchemaExtractor {
        ModelSchema generateModelSchema(String text);
    }


    public ModelSchema build(DbSchema dbSchema, ModelSchemaReq modelSchemaReq) {
        Optional<ChatApp> chatApp = ChatAppManager.getApp(APP_KEY);
        if (!chatApp.isPresent() || !chatApp.get().isEnable()) {
            return null;
        }
        ChatModelConfig chatModelConfig = modelSchemaReq.getChatModelConfig();
        ModelSchemaExtractor extractor =
                AiServices.create(ModelSchemaExtractor.class, getChatModel(chatModelConfig));
        Prompt prompt = generatePrompt(dbSchema, chatApp.get());
        return extractor.generateModelSchema(prompt.toUserMessage().singleText());
    }

    private Prompt generatePrompt(DbSchema dbSchema, ChatApp chatApp) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("exemplar", loadExemplars());
        variable.put("DBSchema", JsonUtil.toString(dbSchema));
        return PromptTemplate.from(chatApp.getPrompt()).apply(variable);
    }

    private String loadExemplars() {
        // to add
        return "";
    }

}
