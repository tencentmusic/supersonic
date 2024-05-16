package dev.langchain4j;

import com.tencent.supersonic.common.pojo.enums.S2ModelProvider;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

class S2EmbeddingModel {

    @NestedConfigurationProperty
    private S2ModelProvider provider;
    @NestedConfigurationProperty
    private OpenAi openAi;
    @NestedConfigurationProperty
    private HuggingFace huggingFace;
    @NestedConfigurationProperty
    private LocalAi localAi;

    @NestedConfigurationProperty
    private InProcess inProcess;

    public S2ModelProvider getProvider() {
        return provider;
    }

    public void setProvider(S2ModelProvider provider) {
        this.provider = provider;
    }

    public OpenAi getOpenAi() {
        return openAi;
    }

    public void setOpenAi(OpenAi openAi) {
        this.openAi = openAi;
    }

    public HuggingFace getHuggingFace() {
        return huggingFace;
    }

    public void setHuggingFace(HuggingFace huggingFace) {
        this.huggingFace = huggingFace;
    }

    public LocalAi getLocalAi() {
        return localAi;
    }

    public void setLocalAi(LocalAi localAi) {
        this.localAi = localAi;
    }

    public InProcess getInProcess() {
        return inProcess;
    }

    public void setInProcess(InProcess inProcess) {
        this.inProcess = inProcess;
    }
}