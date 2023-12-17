package com.tencent.supersonic.common.util.embedding;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/***
 * Implementation of calling the Python service S2EmbeddingStore.
 */
@Slf4j
public class PythonServiceS2EmbeddingStore implements S2EmbeddingStore {

    private RestTemplate restTemplate = new RestTemplate();

    public void addCollection(String collectionName) {
        List<String> collections = getCollectionList();
        if (collections.contains(collectionName)) {
            return;
        }
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        String url = String.format("%s/create_collection?collection_name=%s",
                embeddingConfig.getUrl(), collectionName);
        doRequest(url, null, HttpMethod.GET);
    }

    public void addQuery(String collectionName, List<EmbeddingQuery> queries) {
        if (CollectionUtils.isEmpty(queries)) {
            return;
        }
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        String url = String.format("%s/add_query?collection_name=%s",
                embeddingConfig.getUrl(), collectionName);
        doRequest(url, JSONObject.toJSONString(queries, SerializerFeature.WriteMapNullValue), HttpMethod.POST);
    }

    public void deleteQuery(String collectionName, List<EmbeddingQuery> queries) {
        if (CollectionUtils.isEmpty(queries)) {
            return;
        }
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        List<String> queryIds = queries.stream().map(EmbeddingQuery::getQueryId).collect(Collectors.toList());
        String url = String.format("%s/delete_query_by_ids?collection_name=%s",
                embeddingConfig.getUrl(), collectionName);
        doRequest(url, JSONObject.toJSONString(queryIds), HttpMethod.POST);
    }

    public List<RetrieveQueryResult> retrieveQuery(String collectionName, RetrieveQuery retrieveQuery, int num) {
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        String url = String.format("%s/retrieve_query?collection_name=%s&n_results=%s",
                embeddingConfig.getUrl(), collectionName, num);
        ResponseEntity<String> responseEntity = doRequest(url, JSONObject.toJSONString(retrieveQuery,
                SerializerFeature.WriteMapNullValue), HttpMethod.POST);
        if (!responseEntity.hasBody()) {
            return Lists.newArrayList();
        }
        return JSONObject.parseArray(responseEntity.getBody(), RetrieveQueryResult.class);
    }

    private List<String> getCollectionList() {
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        String url = embeddingConfig.getUrl() + "/list_collections";
        ResponseEntity<String> responseEntity = doRequest(url, null, HttpMethod.GET);
        if (!responseEntity.hasBody()) {
            return Lists.newArrayList();
        }
        List<EmbeddingCollection> embeddingCollections = JSONObject.parseArray(responseEntity.getBody(),
                EmbeddingCollection.class);
        return embeddingCollections.stream().map(EmbeddingCollection::getName).collect(Collectors.toList());
    }

    public ResponseEntity doRequest(String url, String jsonBody, HttpMethod httpMethod) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setLocation(URI.create(url));
            URI requestUrl = UriComponentsBuilder
                    .fromHttpUrl(url).build().encode().toUri();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            if (jsonBody != null) {
                log.info("[embedding] request body :{}", jsonBody);
                entity = new HttpEntity<>(jsonBody, headers);
            }
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl,
                    httpMethod, entity, new ParameterizedTypeReference<String>() {
                    });
            log.info("[embedding] url :{} result body:{}", url, responseEntity);
            return responseEntity;
        } catch (Throwable e) {
            log.warn("doRequest service failed, url:" + url, e);
        }
        return ResponseEntity.of(Optional.empty());
    }
}

