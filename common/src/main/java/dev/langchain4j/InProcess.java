package dev.langchain4j;

class InProcess {

    /***
     * the model local path
     */
    private String modelPath;

    /***
     * the model's vocabulary local path
     */
    private String vocabularyPath;

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getVocabularyPath() {
        return vocabularyPath;
    }

    public void setVocabularyPath(String vocabularyPath) {
        this.vocabularyPath = vocabularyPath;
    }
}