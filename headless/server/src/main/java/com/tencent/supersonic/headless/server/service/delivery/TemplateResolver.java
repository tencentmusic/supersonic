package com.tencent.supersonic.headless.server.service.delivery;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves template variables in message content. Supported variables: - ${reportName} -
 * Report/schedule name - ${executionTime} - Execution timestamp - ${rowCount} - Number of rows in
 * the report - ${downloadUrl} - Download URL for the report file - ${scheduleName} - Schedule name
 * - ${outputFormat} - Output format (XLSX, CSV, etc.)
 */
public class TemplateResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    /**
     * Resolve template variables in the given content.
     *
     * @param template the template string with ${variable} placeholders
     * @param context the delivery context containing variable values
     * @param downloadUrl the constructed download URL (may be null)
     * @return the resolved content with variables replaced
     */
    public static String resolve(String template, DeliveryContext context, String downloadUrl) {
        if (StringUtils.isBlank(template)) {
            return template;
        }

        Map<String, String> variables = buildVariables(context, downloadUrl);

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = variables.getOrDefault(varName, matcher.group(0));
            // Escape special regex characters in replacement
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Build the variables map from delivery context.
     */
    private static Map<String, String> buildVariables(DeliveryContext context, String downloadUrl) {
        Map<String, String> variables = new HashMap<>();

        variables.put("reportName", nullSafe(context.getReportName()));
        variables.put("scheduleName", nullSafe(context.getScheduleName()));
        variables.put("executionTime", nullSafe(context.getExecutionTime()));
        variables.put("rowCount",
                context.getRowCount() != null ? String.valueOf(context.getRowCount()) : "0");
        variables.put("outputFormat", nullSafe(context.getOutputFormat()));
        variables.put("downloadUrl", nullSafe(downloadUrl));

        return variables;
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}
