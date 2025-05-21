package com.tencent.supersonic.headless.chat.parser.llm;

import com.huaban.analysis.jieba.JiebaSegmenter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class TextSimilarityCalculation {
    // 生成词频向量
    private static double[] createVector(List<String> words, List<String> vocabulary) {
        double[] vector = new double[vocabulary.size()];
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String word : words) {
            wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
        }
        for (int i = 0; i < vocabulary.size(); i++) {
            vector[i] = wordFreq.getOrDefault(vocabulary.get(i), 0);
        }
        return vector;
    }
    // 余弦相似度计算公式
    private static double cosineSimilarity(double[] vecA, double[] vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += Math.pow(vecA[i], 2);
            normB += Math.pow(vecB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static double getDataSetSimilarity(String queryText, String datasetName){
        if(queryText ==null || datasetName == null ){ return 0.0;}
        JiebaSegmenter segmenter = new JiebaSegmenter();

        // 1.分词
        List<String> words1 = segmenter.sentenceProcess(queryText);
        List<String> words2 = segmenter.sentenceProcess(datasetName);
        // 2. 构建词汇表并生成向量
        List<String> vocabulary = new ArrayList<>(new HashSet<>(words1));
        vocabulary.addAll(new HashSet<>(words2));

        double[] vector1 = createVector(words1, vocabulary);
        double[] vector2 = createVector(words2, vocabulary);
        // 计算相似度（示例使用简单重叠度计算）
        double similarity = cosineSimilarity(vector1, vector2);
        return similarity;
    }
}
