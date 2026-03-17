package com.tencent.supersonic.feishu.server.render;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.headless.api.pojo.FollowUpHintGenerator;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@Component
@RequiredArgsConstructor
public class FeishuCardRenderer {

    private final FeishuProperties properties;
    private final SensitiveFieldService sensitiveFieldService;

    private static final int MAX_HINTS = 3;

    /**
     * Main render method: converts a QueryResult to a Feishu interactive card JSON.
     */
    public Map<String, Object> renderQueryResult(QueryResult result, SemanticParseInfo parseInfo) {
        if (result == null || result.getQueryState() == QueryState.EMPTY) {
            return renderErrorCard("未找到匹配的数据，请尝试换一种问法");
        }

        if (result.getErrorMsg() != null && !result.getErrorMsg().isEmpty()) {
            return renderErrorCard(result.getErrorMsg());
        }

        List<Map<String, Object>> rows = result.getQueryResults();
        List<QueryColumn> columns = result.getQueryColumns();

        if (rows == null || rows.isEmpty()) {
            return renderEmptyCard(result);
        }

        Map<String, Object> card;
        if (rows.size() == 1 && columns != null && columns.size() <= 2) {
            card = renderSingleValueCard(result);
        } else if (rows.size() <= properties.getMaxTableRows()) {
            card = renderTableCard(result);
        } else {
            card = renderSummaryCard(result);
        }

        // Show warning from textInfo (e.g. trend query with no metrics)
        if (parseInfo != null && parseInfo.getTextInfo() != null
                && parseInfo.getTextInfo().contains("**提示:**")) {
            String textInfo = parseInfo.getTextInfo();
            int hintIdx = textInfo.indexOf("**提示:**");
            if (hintIdx >= 0) {
                String warning = textInfo.substring(hintIdx).trim();
                @SuppressWarnings("unchecked")
                List<Object> elements = (List<Object>) card.get("elements");
                elements.add(0, FeishuCardTemplate.buildMarkdown(warning));
                elements.add(1, FeishuCardTemplate.buildDivider());
            }
        }

        // Add dynamic follow-up hints based on query context and result size
        if (parseInfo != null) {
            List<String> hints = FollowUpHintGenerator.generate(parseInfo, rows.size(),
                    properties.getMaxTableRows());
            if (!hints.isEmpty()) {
                String hintText = "可继续追问，如" + hints.stream().limit(MAX_HINTS)
                        .map(h -> "「" + h + "」").collect(Collectors.joining("、"));
                @SuppressWarnings("unchecked")
                List<Object> elements = (List<Object>) card.get("elements");
                elements.add(FeishuCardTemplate.buildDivider());
                elements.add(FeishuCardTemplate.buildMarkdown(hintText));
            }
        }

        return card;
    }

    /**
     * Table card: shows data as a markdown table with header and footer. Sensitive columns are
     * masked before rendering.
     */
    private Map<String, Object> renderTableCard(QueryResult result) {
        List<QueryColumn> columns = result.getQueryColumns();
        List<Map<String, Object>> rows = result.getQueryResults();

        Map<String, Integer> sensitiveFields = sensitiveFieldService.getSensitiveFields();
        boolean hasMasked = hasSensitiveColumns(columns, sensitiveFields);
        List<Map<String, Object>> maskedRows = maskRows(rows, columns, sensitiveFields);

        List<String> displayNames = columns.stream().map(QueryColumn::getName).toList();
        List<String> dataKeys = columns.stream().map(QueryColumn::getNameEn).toList();

        String tableMarkdown =
                FeishuCardTemplate.buildMarkdownTable(displayNames, dataKeys, maskedRows);

        String footer = String.format("共 %d 条结果", rows.size());
        if (result.getQueryTimeCost() != null) {
            footer += String.format("  |  耗时 %dms", result.getQueryTimeCost());
        }
        if (hasMasked) {
            footer += "\n\n*部分敏感字段已脱敏*";
        }

        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown(tableMarkdown));
        elements.add(FeishuCardTemplate.buildDivider());
        elements.add(FeishuCardTemplate.buildMarkdown(footer));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("查询结果", "blue"));
        card.put("elements", elements);
        return card;
    }

    /**
     * Summary card: shows row count and hint for large result sets.
     */
    private Map<String, Object> renderSummaryCard(QueryResult result) {
        List<Map<String, Object>> rows = result.getQueryResults();

        String content = String.format(
                "查询返回 **%d** 条结果，超出卡片显示上限（%d 条）。\n\n" + "输入 `/export` 或 `导出` 可导出完整数据。", rows.size(),
                properties.getMaxTableRows());

        if (result.getQueryTimeCost() != null) {
            content += String.format("\n\n耗时 %dms", result.getQueryTimeCost());
        }

        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown(content));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("查询结果", "blue"));
        card.put("elements", elements);
        return card;
    }

    /**
     * Single value card (1 row, 1-2 columns): shows as a big number display. Sensitive columns are
     * masked before rendering.
     */
    private Map<String, Object> renderSingleValueCard(QueryResult result) {
        List<QueryColumn> columns = result.getQueryColumns();
        Map<String, Object> row = result.getQueryResults().get(0);

        Map<String, Integer> sensitiveFields = sensitiveFieldService.getSensitiveFields();
        boolean hasMasked = hasSensitiveColumns(columns, sensitiveFields);

        StringBuilder content = new StringBuilder();
        for (QueryColumn col : columns) {
            Object val = row.get(col.getNameEn());
            String valStr = val != null ? val.toString() : "-";
            Integer level = sensitiveFields.get(col.getNameEn());
            valStr = maskValue(valStr, level);
            content.append(String.format("**%s**: %s\n", col.getName(), valStr));
        }

        if (result.getQueryTimeCost() != null) {
            content.append(String.format("\n耗时 %dms", result.getQueryTimeCost()));
        }
        if (hasMasked) {
            content.append("\n\n*部分敏感字段已脱敏*");
        }

        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown(content.toString()));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("查询结果", "green"));
        card.put("elements", elements);
        return card;
    }

    /**
     * Error card with red header.
     */
    public Map<String, Object> renderErrorCard(String message) {
        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown(message));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("查询失败", "red"));
        card.put("elements", elements);
        return card;
    }

    /**
     * Empty result card.
     */
    private Map<String, Object> renderEmptyCard(QueryResult result) {
        String content = "查询未返回任何数据。";
        if (result.getQueryTimeCost() != null) {
            content += String.format("\n\n耗时 %dms", result.getQueryTimeCost());
        }

        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown(content));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("查询结果", "orange"));
        card.put("elements", elements);
        return card;
    }

    /**
     * Help card with usage instructions.
     */
    public Map<String, Object> renderHelpCard(String agentName, List<String> examples) {
        StringBuilder content = new StringBuilder("**直接输入问题即可查询数据**");
        if (agentName != null && !agentName.isEmpty()) {
            content.append("（当前数据域: ").append(agentName).append("）");
        }

        if (examples != null && !examples.isEmpty()) {
            content.append("，例如：\n\n");
            for (String ex : examples.stream().limit(4).toList()) {
                content.append("- ").append(ex).append("\n");
            }
        } else {
            content.append("\n\n");
        }

        content.append("\n**支持的命令：**\n\n").append("| 命令 | 说明 |\n").append("| --- | --- |\n")
                .append("| `/help` 或 `帮助` | 显示此帮助信息 |\n")
                .append("| `/export` 或 `导出` | 导出最近查询结果 |\n")
                .append("| `/history` 或 `历史` | 查看最近查询记录 |\n")
                .append("| `/template` 或 `模板` | 查看可用模板列表 |\n")
                .append("| `/use <编号>` | 切换数据域，如 `/use 3` |\n")
                .append("| `/sql <查询>` | 预览生成的 SQL |");

        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown(content.toString()));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("使用帮助", "blue"));
        card.put("elements", elements);
        return card;
    }

    /**
     * SQL preview card: shows the generated SQL without executing.
     */
    public Map<String, Object> renderSqlPreviewCard(String queryText, SemanticParseInfo parseInfo) {
        StringBuilder content = new StringBuilder();

        content.append(String.format("**查询**: %s\n", queryText));

        if (parseInfo.getDataSet() != null && parseInfo.getDataSet().getName() != null) {
            content.append(String.format("**数据集**: %s\n", parseInfo.getDataSet().getName()));
        }

        content.append(String.format("**置信度**: %.2f\n", parseInfo.getScore()));

        // Get the best available SQL: correctedS2SQL > parsedS2SQL > querySQL
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        String sql = null;
        if (sqlInfo != null) {
            sql = sqlInfo.getCorrectedS2SQL();
            if (sql == null || sql.isBlank()) {
                sql = sqlInfo.getParsedS2SQL();
            }
            if (sql == null || sql.isBlank()) {
                sql = sqlInfo.getQuerySQL();
            }
        }

        if (sql != null && !sql.isBlank()) {
            content.append(String.format("\n```sql\n%s\n```\n", sql.trim()));
        } else {
            content.append("\n*未生成 SQL*\n");
        }

        content.append("\n直接发送查询文本可执行此 SQL");

        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown(content.toString()));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("SQL 预览", "blue"));
        card.put("elements", elements);
        return card;
    }

    /**
     * Agent list card: shows available agents with descriptions and example questions.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> renderAgentListCard(List<Map<String, Object>> agents,
            Integer currentAgentId) {
        List<Object> elements = new ArrayList<>();

        for (int i = 0; i < agents.size(); i++) {
            Map<String, Object> agent = agents.get(i);
            Object id = agent.get("id");
            String name = agent.get("name") != null ? agent.get("name").toString() : "未命名";
            String desc =
                    agent.get("description") != null ? agent.get("description").toString() : "";
            if (desc.length() > 80) {
                desc = desc.substring(0, 80) + "...";
            }

            boolean isCurrent = currentAgentId != null
                    && currentAgentId.equals(id instanceof Number ? ((Number) id).intValue() : id);

            StringBuilder content = new StringBuilder();
            int agentId = id instanceof Number ? ((Number) id).intValue() : 0;
            content.append(String.format("**%d. %s**", i + 1, name));
            if (isCurrent) {
                content.append("  *(当前)*");
            } else {
                content.append(String.format("  `/use %d`", agentId));
            }
            if (!desc.isEmpty()) {
                content.append(String.format("\n%s", desc));
            }

            // Show up to 2 example questions
            Object examplesObj = agent.get("examples");
            if (examplesObj instanceof List) {
                List<String> examples = (List<String>) examplesObj;
                List<String> shown = examples.stream().limit(2).toList();
                if (!shown.isEmpty()) {
                    content.append("\n试试问: ");
                    content.append(shown.stream().map(e -> "\"" + e + "\"")
                            .collect(Collectors.joining("、")));
                }
            }

            elements.add(FeishuCardTemplate.buildMarkdown(content.toString()));
            if (i < agents.size() - 1) {
                elements.add(FeishuCardTemplate.buildDivider());
            }
        }

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("可用查询模板", "blue"));
        card.put("elements", elements);
        return card;
    }

    /**
     * Bind guide card: shown when auto-match fails and OAuth binding is enabled.
     */
    public Map<String, Object> renderBindGuideCard(String feishuUserName, String bindUrl) {
        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown("您好，**" + feishuUserName + "**！\n\n"
                + "系统未能自动识别您的数据平台账号。" + "请点击下方按钮，输入您的平台账号密码完成绑定（仅需一次）。"));
        elements.add(FeishuCardTemplate.buildDivider());
        elements.add(FeishuCardTemplate.buildUrlButton("绑定账号", bindUrl, "primary"));
        elements.add(FeishuCardTemplate.buildNote("绑定链接 30 分钟内有效。如无平台账号，请联系管理员开通。"));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("账号绑定", "orange"));
        card.put("elements", elements);
        return card;
    }

    // ── Sensitive field masking helpers ─────────────────────────────────────────

    /**
     * Returns true if any column in the list has a HIGH or MID sensitive level.
     */
    private boolean hasSensitiveColumns(List<QueryColumn> columns,
            Map<String, Integer> sensitiveFields) {
        if (columns == null || sensitiveFields.isEmpty()) {
            return false;
        }
        return columns.stream().anyMatch(col -> sensitiveFields.containsKey(col.getNameEn()));
    }

    /**
     * Returns a copy of {@code rows} with sensitive column values replaced by masked strings.
     * Original rows are not modified.
     */
    private List<Map<String, Object>> maskRows(List<Map<String, Object>> rows,
            List<QueryColumn> columns, Map<String, Integer> sensitiveFields) {
        if (sensitiveFields.isEmpty()) {
            return rows;
        }
        return rows.stream().map(row -> {
            Map<String, Object> maskedRow = new HashMap<>(row);
            for (QueryColumn col : columns) {
                Integer level = sensitiveFields.get(col.getNameEn());
                if (level != null) {
                    Object val = maskedRow.get(col.getNameEn());
                    String original = val != null ? val.toString() : "";
                    maskedRow.put(col.getNameEn(), maskValue(original, level));
                }
            }
            return maskedRow;
        }).collect(Collectors.toList());
    }

    /**
     * Masks a single value according to its sensitive level.
     * <ul>
     * <li>HIGH (code 2): replaced entirely with {@code "***"}</li>
     * <li>MID (code 1): first character + {@code "***"} + last character, e.g. {@code "张***明"}</li>
     * <li>null / LOW: returned unchanged</li>
     * </ul>
     */
    private String maskValue(String value, Integer sensitiveLevel) {
        if (sensitiveLevel == null || value == null || value.isEmpty()) {
            return value;
        }
        if (SensitiveLevelEnum.HIGH.getCode().equals(sensitiveLevel)) {
            return "***";
        }
        if (SensitiveLevelEnum.MID.getCode().equals(sensitiveLevel)) {
            if (value.length() == 1) {
                return "*";
            }
            if (value.length() == 2) {
                return value.charAt(0) + "*";
            }
            return value.charAt(0) + "***" + value.charAt(value.length() - 1);
        }
        return value;
    }

    /**
     * History card showing recent query sessions.
     */
    public Map<String, Object> renderHistoryCard(List<Map<String, String>> items) {
        StringBuilder content = new StringBuilder("**最近查询记录：**\n\n");

        for (int i = 0; i < items.size(); i++) {
            Map<String, String> item = items.get(i);
            String status = item.getOrDefault("status", "");
            String statusIcon = switch (status) {
                case "SUCCESS" -> "[完成]";
                case "ERROR" -> "[失败]";
                case "PENDING" -> "[处理中]";
                default -> "[" + status + "]";
            };
            content.append(String.format("%d. %s %s  *%s*\n", i + 1, statusIcon,
                    item.getOrDefault("query", ""), item.getOrDefault("time", "")));
        }

        List<Object> elements = new ArrayList<>();
        elements.add(FeishuCardTemplate.buildMarkdown(content.toString()));

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader("查询历史", "blue"));
        card.put("elements", elements);
        return card;
    }
}
