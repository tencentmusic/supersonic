package dev.langchain4j.store.embedding;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class GsonInMemoryEmbeddingStoreJsonCodec implements InMemoryEmbeddingStoreJsonCodec {

    @Override
    public InMemoryS2EmbeddingStore.InMemoryEmbeddingStore<EmbeddingQuery> fromJson(String json) {
        Type type = new TypeToken<InMemoryS2EmbeddingStore.InMemoryEmbeddingStore<EmbeddingQuery>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }

    @Override
    public String toJson(InMemoryS2EmbeddingStore.InMemoryEmbeddingStore<?> store) {
        return new Gson().toJson(store);
    }
}
