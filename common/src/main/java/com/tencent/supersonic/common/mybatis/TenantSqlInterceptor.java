package com.tencent.supersonic.common.mybatis;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * MyBatis interceptor that automatically injects tenant_id conditions into SQL statements. Supports
 * SELECT, UPDATE, DELETE operations by adding WHERE clause conditions. For INSERT, the tenant_id
 * should be set in the entity before insertion.
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare",
        args = {Connection.class, Integer.class})})
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class TenantSqlInterceptor implements Interceptor {

    private static final String TENANT_ID_COLUMN = "tenant_id";

    /**
     * Default tables to exclude from tenant filtering (tables without tenant_id column). This
     * serves as a fallback when TenantConfig is not available.
     */
    private static final Set<String> DEFAULT_EXCLUDED_TABLES =
            new HashSet<>(Arrays.asList("s2_tenant", "s2_subscription_plan", "s2_permission",
                    "s2_role_permission", "s2_user_role"));

    private TenantConfig tenantConfig;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Long tenantId = TenantContext.getTenantId();

        // Skip if no tenant context
        if (tenantId == null) {
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // Get MappedStatement to check SQL command type
        MappedStatement mappedStatement =
                (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        // Get BoundSql
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        // Process SQL based on command type
        String modifiedSql = processSql(originalSql, sqlCommandType, tenantId);

        if (modifiedSql != null && !modifiedSql.equals(originalSql)) {
            // Update the SQL in BoundSql
            setSqlField(boundSql, modifiedSql);
            log.debug("Modified SQL with tenant_id={}: {}", tenantId, modifiedSql);
        }

        return invocation.proceed();
    }

    private String processSql(String sql, SqlCommandType sqlCommandType, Long tenantId) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);

            switch (sqlCommandType) {
                case SELECT:
                    if (statement instanceof Select) {
                        processSelect((Select) statement, tenantId);
                    }
                    break;
                case UPDATE:
                    if (statement instanceof Update) {
                        processUpdate((Update) statement, tenantId);
                    }
                    break;
                case DELETE:
                    if (statement instanceof Delete) {
                        processDelete((Delete) statement, tenantId);
                    }
                    break;
                case INSERT:
                    // INSERT statements should have tenant_id set in the entity
                    // We don't modify INSERT statements here
                    return sql;
                default:
                    return sql;
            }

            return statement.toString();
        } catch (JSQLParserException e) {
            log.warn("Failed to parse SQL for tenant filtering: {}", e.getMessage());
            return sql;
        }
    }

    private void processSelect(Select select, Long tenantId) {
        if (select instanceof PlainSelect) {
            processPlainSelect((PlainSelect) select, tenantId);
        } else if (select instanceof SetOperationList setOperationList) {
            for (Select body : setOperationList.getSelects()) {
                processSelect(body, tenantId);
            }
        }
    }

    private void processPlainSelect(PlainSelect plainSelect, Long tenantId) {
        FromItem fromItem = plainSelect.getFromItem();

        if (fromItem instanceof Table table) {
            if (!shouldExcludeTable(table.getName())) {
                Expression where = plainSelect.getWhere();
                Expression tenantCondition = createTenantCondition(table, tenantId);
                plainSelect.setWhere(combineConditions(where, tenantCondition));
            }
        } else if (fromItem instanceof ParenthesedSelect subSelect) {
            processSelect(subSelect.getSelect(), tenantId);
        }

        // Process JOINs
        if (plainSelect.getJoins() != null) {
            plainSelect.getJoins().forEach(join -> {
                if (join.getRightItem()instanceof Table joinTable) {
                    if (!shouldExcludeTable(joinTable.getName())) {
                        Expression tenantCondition = createTenantCondition(joinTable, tenantId);
                        if (join.getOnExpression() != null) {
                            join.setOnExpression(
                                    combineConditions(join.getOnExpression(), tenantCondition));
                        }
                    }
                } else if (join.getRightItem()instanceof ParenthesedSelect subSelect) {
                    processSelect(subSelect.getSelect(), tenantId);
                }
            });
        }
    }

    private void processUpdate(Update update, Long tenantId) {
        Table table = update.getTable();
        if (!shouldExcludeTable(table.getName())) {
            Expression where = update.getWhere();
            Expression tenantCondition = createTenantCondition(table, tenantId);
            update.setWhere(combineConditions(where, tenantCondition));
        }
    }

    private void processDelete(Delete delete, Long tenantId) {
        Table table = delete.getTable();
        if (!shouldExcludeTable(table.getName())) {
            Expression where = delete.getWhere();
            Expression tenantCondition = createTenantCondition(table, tenantId);
            delete.setWhere(combineConditions(where, tenantCondition));
        }
    }

    private Expression createTenantCondition(Table table, Long tenantId) {
        Column column = new Column();
        if (table.getAlias() != null) {
            column.setTable(new Table(table.getAlias().getName()));
        } else {
            column.setTable(table);
        }
        column.setColumnName(TENANT_ID_COLUMN);

        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(new LongValue(tenantId));

        return equalsTo;
    }

    private Expression combineConditions(Expression original, Expression additional) {
        if (original == null) {
            return additional;
        }
        return new AndExpression(original, additional);
    }

    private boolean shouldExcludeTable(String tableName) {
        // Clean table name (remove backticks, schema prefix, etc.)
        String cleanName = cleanTableName(tableName);

        // Quartz system tables (QRTZ_*) have no tenant_id column — must never
        // be rewritten by this interceptor, otherwise the clustered scheduler
        // breaks on every acquire/fire/checkin query.
        if (cleanName != null && cleanName.length() >= 5
                && cleanName.regionMatches(true, 0, "QRTZ_", 0, 5)) {
            log.debug("Table '{}' is a Quartz system table, excluded", cleanName);
            return true;
        }

        // First check default excluded tables (always applied as safety net)
        if (cleanName != null
                && DEFAULT_EXCLUDED_TABLES.stream().anyMatch(cleanName::equalsIgnoreCase)) {
            log.debug("Table '{}' is in default excluded list", cleanName);
            return true;
        }

        // Try to get TenantConfig lazily if not injected
        TenantConfig config = this.tenantConfig;
        if (config == null) {
            try {
                config = ContextUtils.getBean(TenantConfig.class);
            } catch (Exception e) {
                log.debug("TenantConfig not available from context");
            }
        }

        if (config != null) {
            boolean excluded = config.isExcludedTable(cleanName);
            if (excluded) {
                log.debug("Table '{}' is excluded by TenantConfig", cleanName);
            }
            return excluded;
        }

        return false;
    }

    private String cleanTableName(String tableName) {
        if (tableName == null) {
            return null;
        }
        // Remove backticks, quotes, and schema prefix
        String clean = tableName.replace("`", "").replace("\"", "").replace("'", "").trim();
        // Remove schema prefix if present (e.g., "schema.table" -> "table")
        int dotIndex = clean.lastIndexOf('.');
        if (dotIndex >= 0) {
            clean = clean.substring(dotIndex + 1);
        }
        return clean;
    }

    private void setSqlField(BoundSql boundSql, String sql) {
        try {
            Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, sql);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed to set SQL field in BoundSql", e);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // Properties can be configured in mybatis-config.xml if needed
    }
}
