package dev.langchain4j.provider;

import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.BgeSmallZhEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingModelConstant {

    public static final String BGE_SMALL_ZH = "bge-small-zh";
    public static final String ALL_MINILM_L6_V2 = "all-minilm-l6-v2-q";
    public static final EmbeddingModel BGE_SMALL_ZH_MODEL = new BgeSmallZhEmbeddingModel();
    public static final EmbeddingModel ALL_MINI_LM_L6_V2_MODEL =
            new AllMiniLmL6V2QuantizedEmbeddingModel();
}
