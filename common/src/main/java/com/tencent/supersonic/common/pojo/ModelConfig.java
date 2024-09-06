package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private ChatModelConfig chatModel;
    private EmbeddingModelConfig embeddingModel;
}
