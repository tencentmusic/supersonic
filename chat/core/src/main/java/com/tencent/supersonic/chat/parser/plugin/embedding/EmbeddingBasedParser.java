package com.tencent.supersonic.chat.parser.plugin.embedding;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.parser.ParseMode;
import com.tencent.supersonic.chat.parser.plugin.PluginParser;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginRecallResult;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

@Slf4j
public class EmbeddingBasedParser extends PluginParser {

    @Override
    public boolean checkPreCondition(QueryContext queryContext) {
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        if (StringUtils.isBlank(embeddingConfig.getUrl())) {
            return false;
        }
        List<Plugin> plugins = getPluginList(queryContext);
        return !CollectionUtils.isEmpty(plugins);
    }

    @Override
    public PluginRecallResult recallPlugin(QueryContext queryContext) {
        String text = queryContext.getRequest().getQueryText();
        List<RecallRetrieval> embeddingRetrievals = embeddingRecall(text);
        if (CollectionUtils.isEmpty(embeddingRetrievals)) {
            return null;
        }
        List<Plugin> plugins = getPluginList(queryContext);
        Map<Long, Plugin> pluginMap = plugins.stream().collect(Collectors.toMap(Plugin::getId, p -> p));
        for (RecallRetrieval embeddingRetrieval : embeddingRetrievals) {
            Plugin plugin = pluginMap.get(Long.parseLong(embeddingRetrieval.getId()));
            if (plugin == null) {
                continue;
            }
            Pair<Boolean, Set<Long>> pair = PluginManager.resolve(plugin, queryContext);
            log.info("embedding plugin resolve: {}", pair);
            if (pair.getLeft()) {
                Set<Long> modelList = pair.getRight();
                if (CollectionUtils.isEmpty(modelList)) {
                    continue;
                }
                plugin.setParseMode(ParseMode.EMBEDDING_RECALL);
                double distance = Double.parseDouble(embeddingRetrieval.getDistance());
                double score = queryContext.getRequest().getQueryText().length() * (1 - distance);
                return PluginRecallResult.builder()
                        .plugin(plugin).modelIds(modelList).score(score).distance(distance).build();
            }
        }
        return null;
    }

    public List<RecallRetrieval> embeddingRecall(String embeddingText) {
        try {
            PluginManager pluginManager = ContextUtils.getBean(PluginManager.class);
            EmbeddingResp embeddingResp = pluginManager.recognize(embeddingText);
            List<RecallRetrieval> embeddingRetrievals = embeddingResp.getRetrieval();
            if (!CollectionUtils.isEmpty(embeddingRetrievals)) {
                embeddingRetrievals = embeddingRetrievals.stream().sorted(Comparator.comparingDouble(o ->
                        Math.abs(Double.parseDouble(o.getDistance())))).collect(Collectors.toList());
                embeddingResp.setRetrieval(embeddingRetrievals);
            }
            return embeddingRetrievals;
        } catch (Exception e) {
            log.warn("get embedding result error ", e);
        }
        return Lists.newArrayList();
    }

}
