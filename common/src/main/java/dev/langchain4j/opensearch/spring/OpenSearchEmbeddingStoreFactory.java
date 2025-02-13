package dev.langchain4j.opensearch.spring;

import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.springframework.beans.BeanUtils;

import java.net.URI;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * @author zyc
 */
public class OpenSearchEmbeddingStoreFactory extends BaseEmbeddingStoreFactory {
    private final EmbeddingStoreProperties storeProperties;

    public OpenSearchEmbeddingStoreFactory(EmbeddingStoreConfig storeConfig) {
        this(createPropertiesFromConfig(storeConfig));
    }

    public OpenSearchEmbeddingStoreFactory(EmbeddingStoreProperties storeProperties) {
        this.storeProperties = storeProperties;
    }

    private static EmbeddingStoreProperties createPropertiesFromConfig(
            EmbeddingStoreConfig storeConfig) {
        EmbeddingStoreProperties embeddingStore = new EmbeddingStoreProperties();
        BeanUtils.copyProperties(storeConfig, embeddingStore);
        embeddingStore.setUri(storeConfig.getBaseUrl());
        embeddingStore.setToken(storeConfig.getApiKey());
        embeddingStore.setDatabaseName(storeConfig.getDatabaseName());
        return embeddingStore;
    }

    @Override
    public EmbeddingStore<TextSegment> createEmbeddingStore(String collectionName) {
        final AwsSdk2TransportOptions options =
                AwsSdk2TransportOptions.builder()
                        .setCredentials(StaticCredentialsProvider.create(AwsBasicCredentials
                                .create(storeProperties.getUser(), storeProperties.getPassword())))
                        .build();
        final String indexName = storeProperties.getDatabaseName() + "_" + collectionName;
        return OpenSearchEmbeddingStore.builder().serviceName(storeProperties.getServiceName())
                .serverUrl(storeProperties.getUri()).region(storeProperties.getRegion())
                .indexName(indexName).userName(storeProperties.getUser())
                .password(storeProperties.getPassword()).apiKey(storeProperties.getToken())
                .options(options).build();
    }
}
