package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates contextual follow-up query hints based on a SemanticParseInfo. Hints are natural
 * Chinese phrases that users can directly copy and send as follow-up queries.
 *
 * <p>
 * Hint categories (in priority order):
 * <ol>
 * <li>Dimension drill-down / grouping changes</li>
 * <li>Filter modifications</li>
 * <li>Metric switching</li>
 * <li>Date range changes</li>
 * <li>Result refinement (Top-N, export)</li>
 * </ol>
 */
public final class FollowUpHintGenerator {

    private FollowUpHintGenerator() {}

    /**
     * Generate follow-up hints from a SemanticParseInfo.
     *
     * @param parseInfo the semantic parse result
     * @param resultRowCount the number of rows returned by the query
     * @param maxDisplayRows the max rows the UI can display (rows beyond this trigger export hint)
     * @return ordered list of hint strings (may be empty, never null)
     */
    public static List<String> generate(SemanticParseInfo parseInfo, int resultRowCount,
            int maxDisplayRows) {
        Set<String> hints = new LinkedHashSet<>();

        addDimensionHints(parseInfo, hints);
        addFilterHints(parseInfo, hints);
        addMetricHints(parseInfo, hints);
        addDateHints(parseInfo, hints);
        addResultHints(parseInfo, resultRowCount, maxDisplayRows, hints);

        return List.copyOf(hints);
    }

    /**
     * Convenience overload without result-based hints.
     */
    public static List<String> generate(SemanticParseInfo parseInfo) {
        return generate(parseInfo, 0, Integer.MAX_VALUE);
    }


    /**
     * Dimension hints logic: - If no dimensions used: suggest drilling down by an unused dimension,
     * or switching metric if no dims available. - If 1 dimension used: suggest adding another
     * dimension, or removing the current dimension. - If multiple dimensions used: suggest
     * simplifying to just the first dimension.
     * 
     * @param parseInfo the semantic parse info containing dimensions and metrics
     * @param hints the set to add generated hints to
     */
    private static void addDimensionHints(SemanticParseInfo parseInfo, Set<String> hints) {
        Set<SchemaElement> usedDims = parseInfo.getDimensions();

        Set<Long> usedDimIds = (usedDims == null ? Set.<SchemaElement>of() : usedDims).stream()
                .map(SchemaElement::getId).collect(Collectors.toSet());
        List<String> dimNames = (usedDims == null ? List.<SchemaElement>of() : usedDims).stream()
                .filter(d -> !d.isTimeDimension()).map(SchemaElement::getName)
                .filter(n -> n != null && !n.isEmpty()).toList();

        if (dimNames.isEmpty()) {
            // Pure aggregate — suggest drill-down
            String unusedDim = findUnusedDimensionName(parseInfo, usedDimIds);
            if (unusedDim != null) {
                hints.add("各" + unusedDim + "的呢");
            } else {
                String metricName = getFirstMetricName(parseInfo);
                if (metricName != null) {
                    hints.add("哪个" + metricName + "最高");
                }
            }
        } else if (dimNames.size() == 1) {
            String unusedDim = findUnusedDimensionName(parseInfo, usedDimIds);
            if (unusedDim != null) {
                hints.add("加上" + unusedDim + "看看");
            } else {
                hints.add("看总数");
            }
        } else {
            hints.add("只按" + dimNames.getFirst() + "看");
        }
    }

    /**
     * Find an unused non-time dimension name from the parse info to suggest for drill-down or
     * grouping. Returns the first matching dimension name, or null if none found.
     * 
     * @param parseInfo the semantic parse info containing element matches
     * @param usedDimIds the set of dimension IDs already used in the query
     * @return the name of an unused dimension, or null if none available
     */
    private static String findUnusedDimensionName(SemanticParseInfo parseInfo,
            Set<Long> usedDimIds) {
        List<SchemaElementMatch> matches = parseInfo.getElementMatches();
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.stream().map(SchemaElementMatch::getElement)
                .filter(e -> e.getType() == SchemaElementType.DIMENSION && !e.isTimeDimension())
                .filter(e -> e.getId() != null && !usedDimIds.contains(e.getId()))
                .map(SchemaElement::getName).filter(n -> n != null && !n.isEmpty()).findFirst()
                .orElse(null);
    }

    // ---- Filter hints ----

    private static void addFilterHints(SemanticParseInfo parseInfo, Set<String> hints) {
        Set<QueryFilter> filters = parseInfo.getDimensionFilters();
        if (filters == null || filters.isEmpty()) {
            return;
        }

        for (QueryFilter filter : filters) {
            String name = filter.getName();
            if (name == null || name.isEmpty()) {
                name = filter.getBizName();
            }
            if (name == null || name.isEmpty()) {
                continue;
            }

            Object value = filter.getValue();
            if (value != null) {
                String valueStr = value.toString();
                if (valueStr.length() <= 10) {
                    hints.add("换成全部" + name);
                    return;
                }
            }
            hints.add("换个" + name + "看看");
            return;
        }
    }

    // ---- Metric hints ----

    private static void addMetricHints(SemanticParseInfo parseInfo, Set<String> hints) {
        Set<SchemaElement> metrics = parseInfo.getMetrics();
        if (metrics == null || metrics.size() <= 1) {
            return;
        }

        List<String> metricNames = metrics.stream().map(SchemaElement::getName)
                .filter(n -> n != null && !n.isEmpty()).toList();
        if (metricNames.size() > 1) {
            hints.add("只看" + metricNames.get(1));
        }
    }

    // ---- Date hints ----

    private static void addDateHints(SemanticParseInfo parseInfo, Set<String> hints) {
        DateConf dateInfo = parseInfo.getDateInfo();
        if (dateInfo == null) {
            return;
        }

        DateConf.DateMode mode = dateInfo.getDateMode();
        if (mode == DateConf.DateMode.RECENT) {
            Integer unit = dateInfo.getUnit();
            DatePeriodEnum period = dateInfo.getPeriod();
            if (unit != null && period != null) {
                String suggestion = suggestAlternateRecent(unit, period);
                if (suggestion != null) {
                    hints.add(suggestion);
                }
            }
        } else if (mode == DateConf.DateMode.BETWEEN || mode == DateConf.DateMode.AVAILABLE) {
            hints.add("看看最近7天的");
        }
    }

    private static String suggestAlternateRecent(int unit, DatePeriodEnum period) {
        return switch (period) {
            case DAY -> unit <= 1 ? "看看最近7天的" : unit <= 7 ? "看看最近30天的" : "看看昨天的";
            case WEEK -> "看看最近一个月的";
            case MONTH -> unit <= 1 ? "看看最近3个月的" : "看看上个月的";
            default -> null;
        };
    }

    /**
     * Result-based hints logic: - If result has more rows than max display limit: suggest exporting
     * the data. - If result has more than 5 rows and no limit was applied: suggest looking at just
     * the top 10 rows.
     * 
     * @param parseInfo the semantic parse info containing metrics and limit info (used for top-N
     *        hint)
     * @param resultRowCount the number of rows returned by the query (used to determine if export
     *        hint is needed)
     * @param maxDisplayRows the maximum number of rows the UI can display without scrolling (used
     *        to determine if export hint is needed)
     * @param hints the set to add generated hints to
     */
    private static void addResultHints(SemanticParseInfo parseInfo, int resultRowCount,
            int maxDisplayRows, Set<String> hints) {
        if (resultRowCount <= 5) {
            return;
        }
        String metricName = getFirstMetricName(parseInfo);
        if (resultRowCount > maxDisplayRows) {
            hints.add("导出");
        } else if (parseInfo.getLimit() <= 0 && metricName != null) {
            hints.add("看前10名");
        }
    }

    /**
     * Get the name of the first metric used in the query, or null if no metrics. Used for
     * generating metric-related hints when multiple metrics are present.
     * 
     * @param parseInfo the semantic parse info containing metrics
     * @return the name of the first metric, or null if none available
     */
    private static String getFirstMetricName(SemanticParseInfo parseInfo) {
        Set<SchemaElement> metrics = parseInfo.getMetrics();
        if (metrics == null || metrics.isEmpty()) {
            return null;
        }
        return metrics.iterator().next().getName();
    }
}
