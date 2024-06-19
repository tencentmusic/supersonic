package dev.langchain4j.model.embedding;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;

/**
 * An embedding model that runs within your Java application's process.
 * Any BERT-based model (e.g., from HuggingFace) can be used, as long as it is in ONNX format.
 * Information on how to convert models into ONNX format can be found <a
 * href="https://huggingface.co/docs/optimum/exporters/onnx/usage_guides/export_a_model">here</a>.
 * Many models already converted to ONNX format are available <a href="https://huggingface.co/Xenova">here</a>.
 * Copy from dev.langchain4j.model.embedding.OnnxEmbeddingModel.
 */
public class S2OnnxEmbeddingModel extends AbstractInProcessEmbeddingModel {

    private final OnnxBertBiEncoder model;

    /**
     * @param pathToModel The path to the .onnx model file (e.g., "/home/me/model.onnx").
     */
    public S2OnnxEmbeddingModel(String pathToModel, String vocabularyPath) {
        URL resource = AbstractInProcessEmbeddingModel.class.getResource("/bert-vocabulary-en.txt");
        if (StringUtils.isNotBlank(vocabularyPath)) {
            try {
                resource = Paths.get(vocabularyPath).toUri().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        model = loadFromFileSystem(Paths.get(pathToModel), resource);
    }

    /**
     * @param pathToModel The path to the .onnx model file (e.g., "/home/me/model.onnx").
     */
    public S2OnnxEmbeddingModel(String pathToModel) {
        this(pathToModel, null);
    }

    @Override
    protected OnnxBertBiEncoder model() {
        return model;
    }

    static OnnxBertBiEncoder loadFromFileSystem(Path pathToModel, URL vocabularyFile) {
        try {
            return new OnnxBertBiEncoder(
                    Files.newInputStream(pathToModel),
                    vocabularyFile,
                    PoolingMode.MEAN
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
