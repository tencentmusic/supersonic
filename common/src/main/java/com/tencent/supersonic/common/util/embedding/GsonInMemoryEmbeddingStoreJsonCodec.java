package com.tencent.supersonic.common.util.embedding;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.supersonic.common.util.embedding.InMemoryS2EmbeddingStore.InMemoryEmbeddingStore;
import java.lang.reflect.Type;

public class GsonInMemoryEmbeddingStoreJsonCodec implements InMemoryEmbeddingStoreJsonCodec {

    @Override
    public InMemoryEmbeddingStore<EmbeddingQuery> fromJson(String json) {
        Type type = new TypeToken<InMemoryEmbeddingStore<EmbeddingQuery>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }

    @Override
    public String toJson(InMemoryEmbeddingStore<?> store) {
        return new Gson().toJson(store);
    }
}
