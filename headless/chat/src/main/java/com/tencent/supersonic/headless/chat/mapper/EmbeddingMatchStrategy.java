package com.tencent.supersonic.headless.chat.mapper;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.EmbeddingResult;
import com.tencent.supersonic.headless.chat.knowledge.MetaEmbeddingService;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.*;

/**
 * EmbeddingMatchStrategy uses vector database to perform similarity search against the embeddings
 * of schema elements.
 */
@Service
@Slf4j
public class EmbeddingMatchStrategy extends BatchMatchStrategy<EmbeddingResult> {

    @Autowired
    protected MapperConfig mapperConfig;

    @Autowired
    private MetaEmbeddingService metaEmbeddingService;

    private static final String LLM_FILTER_PROMPT =
            """
                    \
                    #Role: You are a professional data analyst specializing in metrics and dimensions.
                    #Task: Given a user query and a list of retrieved metrics/dimensions through vector recall,
                    please analyze which metrics/dimensions the user is most likely interested in.
                    #Rules:
                    1. Based on user query and retrieved info, accurately determine metrics/dimensions user truly cares about.
                    2. Do not return all retrieved info, only select those highly relevant to user query.
                    3. Maintain high quality output, exclude metrics/dimensions irrelevant to user intent.
                    4. Output must be in JSON array format, only include IDs from retrieved info, e.g.: ['id1', 'id2']
                    5. Return JSON content directly without markdown formatting
                    #Input Example:
                    #User Query: {{userText}}
                    #Retrieved Metrics/Dimensions: {{retrievedInfo}}
                    #Output:""";

    @Override
    public List<EmbeddingResult> detect(ChatQueryContext chatQueryContext, List<S2Term> terms,
            Set<Long> detectDataSetIds) {
        if (chatQueryContext == null || CollectionUtils.isEmpty(detectDataSetIds)) {
            log.warn("Invalid input parameters: context={}, dataSetIds={}", chatQueryContext,
                    detectDataSetIds);
            return Collections.emptyList();
        }

        // 1. Base detection
        List<EmbeddingResult> baseResults = super.detect(chatQueryContext, terms, detectDataSetIds);

        boolean useLLM =
                Boolean.parseBoolean(mapperConfig.getParameterValue(EMBEDDING_MAPPER_USE_LLM));

        // 2. LLM enhanced detection
        if (useLLM) {
            List<EmbeddingResult> llmResults = detectWithLLM(chatQueryContext, detectDataSetIds);
            if (!CollectionUtils.isEmpty(llmResults)) {
                baseResults.addAll(llmResults);
            }
        }

        // 3. Deduplicate results
        return baseResults.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Perform enhanced detection using LLM
     */
    private List<EmbeddingResult> detectWithLLM(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds) {
        try {
            String queryText = chatQueryContext.getRequest().getQueryText();
            if (StringUtils.isBlank(queryText)) {
                return Collections.emptyList();
            }

            // Get segmentation results
            Set<String> detectSegments = extractValidSegments(queryText);
            if (CollectionUtils.isEmpty(detectSegments)) {
                log.info("No valid segments found for text: {}", queryText);
                return Collections.emptyList();
            }

            return detectByBatch(chatQueryContext, detectDataSetIds, detectSegments, true);
        } catch (Exception e) {
            log.error("Error in LLM detection for context: {}", chatQueryContext, e);
            return Collections.emptyList();
        }
    }

    /**
     * Extract valid word segments by filtering out unwanted word natures
     */
    private Set<String> extractValidSegments(String text) {
        List<String> natureList = Arrays.asList(StringUtils.split(
                mapperConfig.getParameterValue(EMBEDDING_MAPPER_ALLOWED_SEGMENT_NATURE), ","));
        return HanlpHelper.getSegment().seg(text).stream()
                .filter(t -> natureList.stream().noneMatch(nature -> t.nature.startsWith(nature)))
                .map(Term::getWord).collect(Collectors.toSet());
    }

    @Override
    public List<EmbeddingResult> detectByBatch(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds, Set<String> detectSegments) {
        return detectByBatch(chatQueryContext, detectDataSetIds, detectSegments, false);
    }

    /**
     * Process detection in batches with LLM option
     *
     * @param chatQueryContext The context of the chat query
     * @param detectDataSetIds Target dataset IDs for detection
     * @param detectSegments Segments to be detected
     * @param useLlm Whether to use LLM for filtering results
     * @return List of embedding results
     */
    public List<EmbeddingResult> detectByBatch(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds, Set<String> detectSegments, boolean useLlm) {
        Set<EmbeddingResult> results = ConcurrentHashMap.newKeySet();
        int embeddingMapperBatch = Integer
                .valueOf(mapperConfig.getParameterValue(MapperConfig.EMBEDDING_MAPPER_BATCH));

        // Process and filter query texts
        List<String> queryTextsList = detectSegments.stream().map(String::trim)
                .filter(StringUtils::isNotBlank).collect(Collectors.toList());

        // Partition queries into sub-lists for batch processing
        List<List<String>> queryTextsSubList =
                Lists.partition(queryTextsList, embeddingMapperBatch);

        // Create and execute tasks for each batch
        List<Callable<Void>> tasks = new ArrayList<>();
        for (List<String> queryTextsSub : queryTextsSubList) {
            tasks.add(
                    createTask(chatQueryContext, detectDataSetIds, queryTextsSub, results, useLlm));
        }
        executeTasks(tasks);

        // Apply LLM filtering if enabled
        if (useLlm) {
            Map<String, Object> variable = new HashMap<>();
            variable.put("userText", chatQueryContext.getRequest().getQueryText());
            variable.put("retrievedInfo", JSONObject.toJSONString(results));

            Prompt prompt = PromptTemplate.from(LLM_FILTER_PROMPT).apply(variable);
            ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel();
            String response = chatLanguageModel.generate(prompt.toUserMessage().singleText());

            if (StringUtils.isBlank(response)) {
                results.clear();
            } else {
                List<String> retrievedIds = JSONObject.parseArray(response, String.class);
                results = results.stream().filter(t -> retrievedIds.contains(t.getId()))
                        .collect(Collectors.toSet());
                results.forEach(r -> r.setLlmMatched(true));
            }
        }

        return new ArrayList<>(results);
    }

    /**
     * Create a task for batch processing
     *
     * @param chatQueryContext The context of the chat query
     * @param detectDataSetIds Target dataset IDs
     * @param queryTextsSub Sub-list of query texts to process
     * @param results Shared result set for collecting results
     * @param useLlm Whether to use LLM
     * @return Callable task
     */
    private Callable<Void> createTask(ChatQueryContext chatQueryContext, Set<Long> detectDataSetIds,
            List<String> queryTextsSub, Set<EmbeddingResult> results, boolean useLlm) {
        return () -> {
            List<EmbeddingResult> oneRoundResults = detectByQueryTextsSub(detectDataSetIds,
                    queryTextsSub, chatQueryContext, useLlm);
            synchronized (results) {
                selectResultInOneRound(results, oneRoundResults);
            }
            return null;
        };
    }

    /**
     * Process a sub-list of query texts
     *
     * @param detectDataSetIds Target dataset IDs
     * @param queryTextsSub Sub-list of query texts
     * @param chatQueryContext Chat query context
     * @param useLlm Whether to use LLM
     * @return List of embedding results for this batch
     */
    private List<EmbeddingResult> detectByQueryTextsSub(Set<Long> detectDataSetIds,
            List<String> queryTextsSub, ChatQueryContext chatQueryContext, boolean useLlm) {
        Map<Long, List<Long>> modelIdToDataSetIds = chatQueryContext.getModelIdToDataSetIds();

        // Get configuration parameters
        double threshold =
                Double.parseDouble(mapperConfig.getParameterValue(EMBEDDING_MAPPER_THRESHOLD));
        int embeddingNumber =
                Integer.parseInt(mapperConfig.getParameterValue(EMBEDDING_MAPPER_NUMBER));
        int embeddingRoundNumber =
                Integer.parseInt(mapperConfig.getParameterValue(EMBEDDING_MAPPER_ROUND_NUMBER));

        // Build and execute query
        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(queryTextsSub).build();
        List<RetrieveQueryResult> retrieveQueryResults = metaEmbeddingService.retrieveQuery(
                retrieveQuery, embeddingNumber, modelIdToDataSetIds, detectDataSetIds);

        if (CollectionUtils.isEmpty(retrieveQueryResults)) {
            return Collections.emptyList();
        }

        // Process results
        List<EmbeddingResult> collect = retrieveQueryResults.stream().peek(result -> {
            if (!useLlm && CollectionUtils.isNotEmpty(result.getRetrieval())) {
                result.getRetrieval()
                        .removeIf(retrieval -> !result.getQuery().contains(retrieval.getQuery())
                                && retrieval.getSimilarity() < threshold);
            }
        }).filter(result -> CollectionUtils.isNotEmpty(result.getRetrieval()))
                .flatMap(result -> result.getRetrieval().stream()
                        .map(retrieval -> convertToEmbeddingResult(result, retrieval)))
                .collect(Collectors.toList());

        // Sort and limit results
        return collect.stream()
                .sorted(Comparator.comparingDouble(EmbeddingResult::getSimilarity).reversed())
                .limit(embeddingRoundNumber * queryTextsSub.size()).collect(Collectors.toList());
    }

    /**
     * Convert RetrieveQueryResult and Retrieval to EmbeddingResult
     *
     * @param queryResult The query result containing retrieval information
     * @param retrieval The retrieval data to be converted
     * @return Converted EmbeddingResult
     */
    private EmbeddingResult convertToEmbeddingResult(RetrieveQueryResult queryResult,
            Retrieval retrieval) {
        EmbeddingResult embeddingResult = new EmbeddingResult();
        BeanUtils.copyProperties(retrieval, embeddingResult);
        embeddingResult.setDetectWord(queryResult.getQuery());
        embeddingResult.setName(retrieval.getQuery());

        // Convert metadata to string values
        Map<String, String> metadata = retrieval.getMetadata().entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
        embeddingResult.setMetadata(metadata);

        return embeddingResult;
    }
}
