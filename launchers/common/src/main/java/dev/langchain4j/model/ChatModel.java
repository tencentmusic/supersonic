package dev.langchain4j.model;

public enum ChatModel {
    ZHIPU("glm"),
    ALI("qwen");

    private final String modelName;

    private ChatModel(String modelName) {
        this.modelName = modelName;
    }

    public String toString() {
        return this.modelName;
    }

    public static ChatModel from(String stringValue) {
        ChatModel[] var1 = values();
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            ChatModel model = var1[var3];
            if (model.modelName.equals(stringValue)) {
                return model;
            }
        }

        throw new IllegalArgumentException("Unknown role: '" + stringValue + "'");
    }
}
