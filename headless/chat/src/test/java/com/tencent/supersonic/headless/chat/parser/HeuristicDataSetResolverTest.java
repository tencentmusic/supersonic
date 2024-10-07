package com.tencent.supersonic.headless.chat.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.llm.DataSetResolver;
import com.tencent.supersonic.headless.chat.parser.llm.HeuristicDataSetResolver;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class HeuristicDataSetResolverTest {

    private DataSetResolver resolver = new HeuristicDataSetResolver();

    @Test
    public void testMaxDatasetSimilarity() {
        Set<Long> dataSets = Sets.newHashSet(1L, 2L);
        ChatQueryContext chatQueryContext = new ChatQueryContext();
        Map<Long, List<SchemaElementMatch>> dataSet2Matches =
                chatQueryContext.getMapInfo().getDataSetElementMatches();
        List<SchemaElementMatch> matches = Lists.newArrayList();
        matches.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(1L)
                .name("超音数").type(SchemaElementType.DATASET).build()).similarity(1).build());
        matches.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(1L)
                .name("访问次数").type(SchemaElementType.METRIC).build()).similarity(0.5).build());
        dataSet2Matches.put(1L, matches);

        List<SchemaElementMatch> matches2 = Lists.newArrayList();
        matches2.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(2L)
                .name("访问用户数").type(SchemaElementType.METRIC).build()).similarity(1).build());
        matches2.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(2L)
                .name("用户").type(SchemaElementType.DIMENSION).build()).similarity(1).build());
        dataSet2Matches.put(2L, matches2);

        Long resolvedDataset = resolver.resolve(chatQueryContext, dataSets);
        assert resolvedDataset == 1L;
    }

    @Test
    public void testMaxMetricSimilarity() {
        Set<Long> dataSets = Sets.newHashSet(1L, 2L);
        ChatQueryContext chatQueryContext = new ChatQueryContext();
        Map<Long, List<SchemaElementMatch>> dataSet2Matches =
                chatQueryContext.getMapInfo().getDataSetElementMatches();
        List<SchemaElementMatch> matches = Lists.newArrayList();
        matches.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(1L)
                .name("访问次数").type(SchemaElementType.METRIC).build()).similarity(1).build());
        dataSet2Matches.put(1L, matches);

        List<SchemaElementMatch> matches2 = Lists.newArrayList();
        matches2.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(2L)
                .name("访问用户数").type(SchemaElementType.METRIC).build()).similarity(0.6).build());
        matches2.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(2L)
                .name("用户").type(SchemaElementType.DIMENSION).build()).similarity(1).build());
        dataSet2Matches.put(2L, matches2);

        Long resolvedDataset = resolver.resolve(chatQueryContext, dataSets);
        assert resolvedDataset == 1L;
    }

    @Test
    public void testTotalSimilarity() {
        Set<Long> dataSets = Sets.newHashSet(1L, 2L);
        ChatQueryContext chatQueryContext = new ChatQueryContext();
        Map<Long, List<SchemaElementMatch>> dataSet2Matches =
                chatQueryContext.getMapInfo().getDataSetElementMatches();
        List<SchemaElementMatch> matches = Lists.newArrayList();
        matches.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(1L)
                .name("访问次数").type(SchemaElementType.METRIC).build()).similarity(0.8).build());
        matches.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(1L)
                .name("部门").type(SchemaElementType.METRIC).build()).similarity(0.7).build());
        dataSet2Matches.put(1L, matches);

        List<SchemaElementMatch> matches2 = Lists.newArrayList();
        matches2.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(2L)
                .name("访问用户数").type(SchemaElementType.METRIC).build()).similarity(0.8).build());
        matches2.add(SchemaElementMatch.builder().element(SchemaElement.builder().dataSetId(2L)
                .name("用户").type(SchemaElementType.DIMENSION).build()).similarity(1).build());
        dataSet2Matches.put(2L, matches2);

        Long resolvedDataset = resolver.resolve(chatQueryContext, dataSets);
        assert resolvedDataset == 2L;
    }
}
