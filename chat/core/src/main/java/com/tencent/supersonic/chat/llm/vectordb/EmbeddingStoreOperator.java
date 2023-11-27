package com.tencent.supersonic.chat.llm.vectordb;

import com.tencent.supersonic.chat.llm.prompt.SqlExample;
import com.tencent.supersonic.common.util.JsonUtil;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmbeddingStoreOperator {

    @Autowired
    private EmbeddingModel embeddingModel;

    public List<TextSegment> retriever(String text, String collectionName, int maxResults) {
        EmbeddingStore embeddingStore = EmbeddingStoreFactory.create(collectionName);
        EmbeddingStoreRetriever retriever = EmbeddingStoreRetriever.from(embeddingStore, embeddingModel, maxResults);
        return retriever.findRelevant(text);
    }

    public List<String> addAll(List<SqlExample> sqlExamples, String collectionName) {
        List<Embedding> embeddings = new ArrayList<>();
        List<TextSegment> textSegments = new ArrayList<>();

        for (SqlExample sqlExample : sqlExamples) {
            String question = sqlExample.getQuestion();
            Embedding embedding = embeddingModel.embed(question).content();
            embeddings.add(embedding);

            Map<String, String> metaDataMap = JsonUtil.toMap(JsonUtil.toString(sqlExample), String.class,
                    String.class);

            TextSegment textSegment = TextSegment.from(question, new Metadata(metaDataMap));
            textSegments.add(textSegment);
        }
        return addAllInternal(embeddings, textSegments, collectionName);
    }

    private List<String> addAllInternal(List<Embedding> embeddings, List<TextSegment> textSegments,
            String collectionName) {
        EmbeddingStore embeddingStore = EmbeddingStoreFactory.create(collectionName);
        return embeddingStore.addAll(embeddings, textSegments);
    }

}
