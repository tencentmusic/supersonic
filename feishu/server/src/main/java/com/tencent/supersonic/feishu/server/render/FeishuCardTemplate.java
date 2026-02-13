package com.tencent.supersonic.feishu.server.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FeishuCardTemplate {
    private FeishuCardTemplate() {}

    public static Map<String, Object> buildHeader(String title, String template) {
        // template: "blue", "green", "red", "orange", etc.
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> titleObj = new HashMap<>();
        titleObj.put("tag", "plain_text");
        titleObj.put("content", title);
        header.put("title", titleObj);
        header.put("template", template);
        return header;
    }

    public static Map<String, Object> buildMarkdown(String content) {
        Map<String, Object> div = new HashMap<>();
        div.put("tag", "div");
        Map<String, Object> text = new HashMap<>();
        text.put("tag", "lark_md");
        text.put("content", content);
        div.put("text", text);
        return div;
    }

    public static Map<String, Object> buildDivider() {
        Map<String, Object> divider = new HashMap<>();
        divider.put("tag", "hr");
        return divider;
    }

    /**
     * Build a markdown table.
     * 
     * @param displayNames column headers shown to the user (e.g. Chinese names)
     * @param dataKeys keys to extract values from each row map (e.g. nameEn/bizName)
     * @param rows data rows
     */
    public static String buildMarkdownTable(List<String> displayNames, List<String> dataKeys,
            List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        // Header row
        sb.append("| ").append(String.join(" | ", displayNames)).append(" |\n");
        sb.append("| ").append(String.join(" | ", displayNames.stream().map(c -> "---").toList()))
                .append(" |\n");
        // Data rows
        for (Map<String, Object> row : rows) {
            sb.append("| ");
            for (int i = 0; i < dataKeys.size(); i++) {
                Object val = row.get(dataKeys.get(i));
                sb.append(val != null ? val.toString() : "");
                if (i < dataKeys.size() - 1)
                    sb.append(" | ");
            }
            sb.append(" |\n");
        }
        return sb.toString();
    }

    public static Map<String, Object> buildActionButton(String text, String value, String type) {
        // type: "primary", "default", "danger"
        Map<String, Object> action = new HashMap<>();
        action.put("tag", "action");
        List<Object> actions = new ArrayList<>();
        Map<String, Object> button = new HashMap<>();
        button.put("tag", "button");
        Map<String, Object> buttonText = new HashMap<>();
        buttonText.put("tag", "plain_text");
        buttonText.put("content", text);
        button.put("text", buttonText);
        button.put("type", type);
        button.put("value", Map.of("action", value));
        actions.add(button);
        action.put("actions", actions);
        return action;
    }
}
