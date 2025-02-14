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
    private static final String MODULE_NAME = "向量数据库配置";

    public static final Parameter EMBEDDING_STORE_PROVIDER =
            new Parameter("s2.embedding.store.provider", EmbeddingStoreType.IN_MEMORY.name(),
                    "向量库类型", "目前支持四种类型：IN_MEMORY、MILVUS、CHROMA、PGVECTOR、OPENSEARCH", "list",
                    MODULE_NAME, getCandidateValues());

    public static final Parameter EMBEDDING_STORE_BASE_URL =
            new Parameter("s2.embedding.store.base.url", "", "BaseUrl", "", "string", MODULE_NAME,
                    null, getBaseUrlDependency());

    public static final Parameter EMBEDDING_STORE_API_KEY =
            new Parameter("s2.embedding.store.api.key", "", "ApiKey", "", "password", MODULE_NAME,
                    null, getApiKeyDependency());

    public static final Parameter EMBEDDING_STORE_PERSIST_PATH =
            new Parameter("s2.embedding.store.persist.path", "", "持久化路径",
                    "默认不持久化，如需持久化请填写持久化路径。" + "注意：如果变更了向量模型需删除该路径下已保存的文件或修改持久化路径", "string",
                    MODULE_NAME, null, getPathDependency());

    public static final Parameter EMBEDDING_STORE_TIMEOUT =
            new Parameter("s2.embedding.store.timeout", "60", "超时时间(秒)", "", "number", MODULE_NAME);

    public static final Parameter EMBEDDING_STORE_DIMENSION =
            new Parameter("s2.embedding.store.dimension", "", "向量维度", "", "number", MODULE_NAME,
                    null, getDimensionDependency());
    public static final Parameter EMBEDDING_STORE_DATABASE_NAME =
            new Parameter("s2.embedding.store.databaseName", "", "DatabaseName", "", "string",
                    MODULE_NAME, null, getDatabaseNameDependency());

    public static final Parameter EMBEDDING_STORE_POST = new Parameter("s2.embedding.store.port",
            "", "端口", "", "number", MODULE_NAME, null, getPortDependency());

    public static final Parameter EMBEDDING_STORE_USER = new Parameter("s2.embedding.store.user",
            "", "用户名", "", "string", MODULE_NAME, null, getUserDependency());

    public static final Parameter EMBEDDING_STORE_PASSWORD =
            new Parameter("s2.embedding.store.password", "", "密码", "", "password", MODULE_NAME,
                    null, getPasswordDependency());

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(EMBEDDING_STORE_PROVIDER, EMBEDDING_STORE_BASE_URL,
                EMBEDDING_STORE_POST, EMBEDDING_STORE_USER, EMBEDDING_STORE_PASSWORD,
                EMBEDDING_STORE_API_KEY, EMBEDDING_STORE_DATABASE_NAME,
                EMBEDDING_STORE_PERSIST_PATH, EMBEDDING_STORE_TIMEOUT, EMBEDDING_STORE_DIMENSION);
    }

    public EmbeddingStoreConfig convert() {
        String provider = getParameterValue(EMBEDDING_STORE_PROVIDER);
        String baseUrl = getParameterValue(EMBEDDING_STORE_BASE_URL);
        String apiKey = getParameterValue(EMBEDDING_STORE_API_KEY);
        String persistPath = getParameterValue(EMBEDDING_STORE_PERSIST_PATH);
        String timeOut = getParameterValue(EMBEDDING_STORE_TIMEOUT);
        String databaseName = getParameterValue(EMBEDDING_STORE_DATABASE_NAME);
        Integer dimension = null;
        if (StringUtils.isNumeric(getParameterValue(EMBEDDING_STORE_DIMENSION))) {
            dimension = Integer.valueOf(getParameterValue(EMBEDDING_STORE_DIMENSION));
        }
        Integer port = null;
        if (StringUtils.isNumeric(getParameterValue(EMBEDDING_STORE_POST))) {
            port = Integer.valueOf(getParameterValue(EMBEDDING_STORE_POST));
        }
        String user = getParameterValue(EMBEDDING_STORE_USER);
        String password = getParameterValue(EMBEDDING_STORE_PASSWORD);
        return EmbeddingStoreConfig.builder().provider(provider).baseUrl(baseUrl).apiKey(apiKey)
                .persistPath(persistPath).databaseName(databaseName).timeOut(Long.valueOf(timeOut))
                .dimension(dimension).post(port).user(user).password(password).build();
    }

    private static ArrayList<String> getCandidateValues() {
        return Lists.newArrayList(EmbeddingStoreType.IN_MEMORY.name(),
                EmbeddingStoreType.MILVUS.name(), EmbeddingStoreType.CHROMA.name(),
                EmbeddingStoreType.PGVECTOR.name(), EmbeddingStoreType.OPENSEARCH.name());
    }

    private static List<Parameter.Dependency> getBaseUrlDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.MILVUS.name(),
                        EmbeddingStoreType.CHROMA.name(), EmbeddingStoreType.PGVECTOR.name(),
                        EmbeddingStoreType.OPENSEARCH.name()),
                ImmutableMap.of(EmbeddingStoreType.MILVUS.name(), "http://localhost:19530",
                        EmbeddingStoreType.CHROMA.name(), "http://localhost:8000",
                        EmbeddingStoreType.PGVECTOR.name(), "127.0.0.1",
                        EmbeddingStoreType.OPENSEARCH.name(), "http://localhost:9200"));
    }

    private static List<Parameter.Dependency> getApiKeyDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.MILVUS.name()),
                ImmutableMap.of(EmbeddingStoreType.MILVUS.name(), DEMO));
    }

    private static List<Parameter.Dependency> getPathDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.IN_MEMORY.name()),
                ImmutableMap.of(EmbeddingStoreType.IN_MEMORY.name(), ""));
    }

    private static List<Parameter.Dependency> getDimensionDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.MILVUS.name(),
                        EmbeddingStoreType.PGVECTOR.name(), EmbeddingStoreType.OPENSEARCH.name()),
                ImmutableMap.of(EmbeddingStoreType.MILVUS.name(), "384",
                        EmbeddingStoreType.PGVECTOR.name(), "512",
                        EmbeddingStoreType.OPENSEARCH.name(), "512"));
    }

    private static List<Parameter.Dependency> getDatabaseNameDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.MILVUS.name(),
                        EmbeddingStoreType.PGVECTOR.name(), EmbeddingStoreType.OPENSEARCH.name()),
                ImmutableMap.of(EmbeddingStoreType.MILVUS.name(), "",
                        EmbeddingStoreType.PGVECTOR.name(), "postgres",
                        EmbeddingStoreType.OPENSEARCH.name(), "ai_sql"));
    }

    private static List<Parameter.Dependency> getPortDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.PGVECTOR.name()),
                ImmutableMap.of(EmbeddingStoreType.PGVECTOR.name(), "54333"));
    }

    private static List<Parameter.Dependency> getUserDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.MILVUS.name(),
                        EmbeddingStoreType.PGVECTOR.name(), EmbeddingStoreType.OPENSEARCH.name()),
                ImmutableMap.of(EmbeddingStoreType.MILVUS.name(), "milvus",
                        EmbeddingStoreType.PGVECTOR.name(), "postgres",
                        EmbeddingStoreType.OPENSEARCH.name(), "opensearch"));
    }

    private static List<Parameter.Dependency> getPasswordDependency() {
        return getDependency(EMBEDDING_STORE_PROVIDER.getName(),
                Lists.newArrayList(EmbeddingStoreType.MILVUS.name(),
                        EmbeddingStoreType.PGVECTOR.name(), EmbeddingStoreType.OPENSEARCH.name()),
                ImmutableMap.of(EmbeddingStoreType.MILVUS.name(), "milvus",
                        EmbeddingStoreType.PGVECTOR.name(), "postgres",
                        EmbeddingStoreType.OPENSEARCH.name(), "opensearch"));
    }
}
