package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * SQL template configuration for complex reports that cannot be expressed through structured
 * queries (e.g., multi-table UNION, window functions). Uses StringTemplate (ST4) syntax for
 * parameter rendering.
 */
@Data
public class SqlTemplateConfig implements Serializable {

    /**
     * SQL template text using ST4 syntax. Variables use $var$ delimiters. Conditionals use
     * $if(var)$...$endif$ syntax.
     *
     * Example: SELECT city, SUM(gmv) AS gmv FROM dwd_order_di WHERE dt BETWEEN '$start_date$' AND
     * '$end_date$' $if(city)$ AND city IN ($city$) $endif$ GROUP BY city
     */
    private String templateSql;

    /**
     * Template variable definitions. Reuses existing SqlVariable model for type safety and default
     * values.
     */
    private List<SqlVariable> variables;
}
