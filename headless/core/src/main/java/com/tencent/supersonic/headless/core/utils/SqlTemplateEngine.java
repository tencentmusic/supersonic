package com.tencent.supersonic.headless.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.stringtemplate.v4.ST;

import java.util.Map;

/**
 * SQL template rendering engine based on StringTemplate (ST4). Renders parameterized SQL templates
 * with variable substitution and conditional blocks, then validates the output for SQL safety.
 */
@Component
@Slf4j
public class SqlTemplateEngine {

    private static final char DELIMITER = '$';

    /**
     * Render a SQL template with the given parameters.
     *
     * @param templateSql SQL template using ST4 syntax ($var$, $if(var)$...$endif$)
     * @param params parameter name-value map
     * @return rendered SQL string
     * @throws com.tencent.supersonic.common.pojo.exception.InvalidArgumentException if rendered SQL
     *         contains dangerous operations
     */
    public String render(String templateSql, Map<String, Object> params) {
        if (StringUtils.isBlank(templateSql)) {
            throw new IllegalArgumentException("templateSql is required");
        }

        ST st = new ST(templateSql, DELIMITER, DELIMITER);
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach((key, value) -> {
                if (key != null && value != null) {
                    st.add(key, value);
                }
            });
        }

        String rendered = st.render();
        log.debug("Rendered SQL template: {}", rendered);

        // Validate rendered SQL for dangerous operations
        SqlVariableParseUtils.checkSensitiveSql(rendered);

        return rendered;
    }
}
