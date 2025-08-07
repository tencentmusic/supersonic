package com.tencent.supersonic.chat.server.plugin.recognize.embedding;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.ParseMode;
import com.tencent.supersonic.chat.server.plugin.PluginManager;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** EmbeddingRecallParser is an implementation of a recall plugin based on Embedding */
@Slf4j
public class EmbeddingRecallRecognizer extends PluginRecognizer {

    @Override
    protected List<ChatPlugin> getPluginList(ParseContext parseContext) { // 排除react 插件，不在这里处理
        List<ChatPlugin> plugins = super.getPluginList(parseContext);
        plugins = plugins.stream().filter(e -> {
            if (e.getDataSetList().size() == 0)
                return false;
            if (!e.getType().equals("WEB_SERVICE") && !e.getType().equals("WEB_PAGE"))
                return false;
            return true;
        }).collect(Collectors.toList());
        return plugins;
    }

    public boolean checkPreCondition(ParseContext parseContext) {
        List<ChatPlugin> plugins = getPluginList(parseContext);
        return !CollectionUtils.isEmpty(plugins);
    }

    public PluginRecallResult recallPlugin(ParseContext parseContext) {
        String text = parseContext.getRequest().getQueryText();
        List<Retrieval> embeddingRetrievals = embeddingRecall(text);
        if (CollectionUtils.isEmpty(embeddingRetrievals)) {
            return null;
        }
        List<ChatPlugin> plugins = getPluginList(parseContext);
        Map<Long, ChatPlugin> pluginMap =
                plugins.stream().collect(Collectors.toMap(ChatPlugin::getId, p -> p));
        for (Retrieval embeddingRetrieval : embeddingRetrievals) {
            ChatPlugin plugin = pluginMap.get(Long.parseLong(embeddingRetrieval.getId()));
            if (plugin == null) {
                continue;
            }
            Pair<Boolean, Set<Long>> pair = PluginManager.resolve(plugin, parseContext);
            log.info("embedding plugin resolve: {}", pair);
            if (pair.getLeft()) {
                Set<Long> dataSetList = pair.getRight();
                if (CollectionUtils.isEmpty(dataSetList)) {
                    continue;
                }
                plugin.setParseMode(ParseMode.EMBEDDING_RECALL);
                double similarity = embeddingRetrieval.getSimilarity();
                double score = parseContext.getRequest().getQueryText().length() * similarity;
                return PluginRecallResult.builder().plugin(plugin).dataSetIds(dataSetList)
                        .score(score).distance(similarity).build();
            }
        }
        return null;
    }

    public List<Retrieval> embeddingRecall(String embeddingText) {
        try {
            PluginManager pluginManager = ContextUtils.getBean(PluginManager.class);
            RetrieveQueryResult embeddingResp = pluginManager.recognize(embeddingText);

            List<Retrieval> embeddingRetrievals = embeddingResp.getRetrieval();
            if (!CollectionUtils.isEmpty(embeddingRetrievals)) {
                embeddingRetrievals = embeddingRetrievals.stream()
                        .sorted(Comparator.comparingDouble(o -> Math.abs(o.getSimilarity())))
                        .collect(Collectors.toList());
                embeddingResp.setRetrieval(embeddingRetrievals);
            }
            return embeddingRetrievals;
        } catch (Exception e) {
            log.warn("get embedding result error ", e);
        }
        return Lists.newArrayList();
    }
}
