package com.tencent.supersonic.common.jsqlparser;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ParseVisitorHelper {

    public void replaceColumn(Column column, Map<String, String> fieldNameMap, boolean exactReplace) {
        String columnName = column.getColumnName();
        String replaceColumn = getReplaceValue(columnName, fieldNameMap, exactReplace);
        if (StringUtils.isNotBlank(replaceColumn)) {
            column.setColumnName(replaceColumn);
        }
    }

    public String getReplaceValue(String beforeValue, Map<String, String> valueMap, boolean exactReplace) {
        String value = valueMap.get(beforeValue);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }
        if (exactReplace) {
            return null;
        }
        Optional<Entry<String, String>> first = valueMap.entrySet().stream().sorted((k1, k2) -> {
            String k1Value = k1.getKey();
            String k2Value = k2.getKey();
            Double k1Similarity = getSimilarity(beforeValue, k1Value);
            Double k2Similarity = getSimilarity(beforeValue, k2Value);
            return k2Similarity.compareTo(k1Similarity);
        }).collect(Collectors.toList()).stream().findFirst();

        if (first.isPresent()) {
            return first.get().getValue();
        }
        return beforeValue;
    }

    public static int editDistance(String word1, String word2) {
        final int m = word1.length();
        final int n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int j = 0; j <= n; ++j) {
            dp[0][j] = j;
        }
        for (int i = 0; i <= m; ++i) {
            dp[i][0] = i;
        }

        for (int i = 1; i <= m; ++i) {
            char ci = word1.charAt(i - 1);
            for (int j = 1; j <= n; ++j) {
                char cj = word2.charAt(j - 1);
                if (ci == cj) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else if (i > 1 && j > 1 && ci == word2.charAt(j - 2) && cj == word1.charAt(i - 2)) {
                    dp[i][j] = 1 + Math.min(dp[i - 2][j - 2], Math.min(dp[i][j - 1], dp[i - 1][j]));
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + 1, Math.min(dp[i][j - 1] + 1, dp[i - 1][j] + 1));
                }
            }
        }
        return dp[m][n];
    }

    public double getSimilarity(String word1, String word2) {
        return 1 - (double) editDistance(word1, word2) / Math.max(word2.length(), word1.length());
    }
}