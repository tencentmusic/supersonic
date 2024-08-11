package com.tencent.supersonic.common.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.store.embedding.EmbeddingStoreType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("EmbeddingStoreParameterConfig")
@Slf4j
public class EmbeddingStoreParameterConfig extends ParameterConfig {
    public static final Parameter EMBEDDING_STORE_PROVIDER =
            new Parameter("s2.embedding.store.provider", EmbeddingStoreType.IN_MEMORY.name(),
                    "向量库类型", "", "list",
                    "向量库配置", getCandidateValues());

    public static final Parameter EMBEDDING_STORE_BASE_URL =
            new Parameter("s2.embedding.store.base.url", "",
                    "BaseUrl", "", "string",
                    "向量库配置", null, getBaseUrlDependency());

    public static final Parameter EMBEDDING_STORE_API_KEY =
            new Parameter("s2.embedding.store.api.key", "",
                    "ApiKey", "", "password",
                    "向量库配置", null, getApiKeyDependency());

    public static final Parameter EMBEDDING_STORE_PERSIST_PATH =
            new Parameter("s2.embedding.store.persist.path", "/tmp",
                    "持久化路径", "", "string",
                    "向量库配置", null, getPathDependency());

    public static final Parameter EMBEDDING_STORE_TIMEOUT =
            new Parameter("s2.embedding.store.timeout", "60",
                    "超时时间(秒)", "",
                    "number", "向量库配置");

    public static final Parameter EMBEDDING_STORE_DIMENSION =
            new Parameter("s2.embedding.store.dimension", "",
                    "纬度", "", "number",
                    "向量库配置", null, getDimensionDependency());

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(
                EMBEDDING_STORE_PROVIDER, EMBEDDING_STORE_BASE_URL, EMBEDDING_STORE_API_KEY,
                EMBEDDING_STORE_PERSIST_PATH, EMBEDDING_STORE_TIMEOUT, EMBEDDING_STORE_DIMENSION
        );
    }

    public EmbeddingStoreConfig convert() {
        String provider = getParameterValue(EMBEDDING_STORE_PROVIDER);
        String baseUrl = getParameterValue(EMBEDDING_STORE_BASE_URL);
        String apiKey = getParameterValue(EMBEDDING_STORE_API_KEY);
        String persistPath = getParameterValue(EMBEDDING_STORE_PERSIST_PATH);
        String timeOut = getParameterValue(EMBEDDING_STORE_TIMEOUT);
        Integer dimension = null;
        if (StringUtils.isNumeric(getParameterValue(EMBEDDING_STORE_DIMENSION))) {
            dimension = Integer.valueOf(getParameterValue(EMBEDDING_STORE_DIMENSION));
        }
        return EmbeddingStoreConfig.builder().provider(provider)
                .baseUrl(baseUrl).apiKey(apiKey).persistPath(persistPath)
                .timeOut(Long.valueOf(timeOut)).dimension(dimension).build();
    }

    private static ArrayList<String> getCandidateValues() {
        return Lists.newArrayList(
                EmbeddingStoreType.IN_MEMORY.name(),
                EmbeddingStoreType.MILVUS.name(),
                EmbeddingStoreType.CHROMA.name());
    }

    private static List<Parameter.Dependency> getBaseUrlDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(
                        EmbeddingStoreType.MILVUS.name(), EmbeddingStoreType.CHROMA.name()),
                ImmutableMap.of(
                        EmbeddingStoreType.MILVUS.name(), "http://localhost:19530",
                        EmbeddingStoreType.CHROMA.name(), "http://localhost:8000"
                )
        );
    }

    private static List<Parameter.Dependency> getApiKeyDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.MILVUS.name()),
                ImmutableMap.of(EmbeddingStoreType.MILVUS.name(), DEMO)
        );
    }

    private static List<Parameter.Dependency> getPathDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.IN_MEMORY.name()),
                ImmutableMap.of(EmbeddingStoreType.IN_MEMORY.name(), "/tmp"));
    }

    private static List<Parameter.Dependency> getDimensionDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(
                        EmbeddingStoreType.MILVUS.name()
                ),
                ImmutableMap.of(
                        EmbeddingStoreType.MILVUS.name(), "384"
                )
        );
    }
}