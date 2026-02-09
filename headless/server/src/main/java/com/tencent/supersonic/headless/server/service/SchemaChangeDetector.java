package com.tencent.supersonic.headless.server.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema;
import com.tencent.supersonic.headless.server.pojo.SchemaChange;
import com.tencent.supersonic.headless.server.pojo.SchemaChange.ChangeType;
import com.tencent.supersonic.headless.server.pojo.SchemaChange.ColumnChange;
import com.tencent.supersonic.headless.server.pojo.SchemaChange.StreamChange;
import com.tencent.supersonic.headless.server.pojo.SchemaChangeStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects schema changes between configured catalog and discovered schema.
 * <p>
 * Breaking changes (require user intervention): - Removed tables - Removed columns - Column type
 * changes (may cause data loss) - Primary key changes
 * <p>
 * Non-breaking changes (can be auto-applied): - Added tables - Added columns (nullable)
 */
@Component
@Slf4j
public class SchemaChangeDetector {

    /**
     * Detect changes between configured catalog (what we sync) and discovered schema (current
     * state).
     *
     * @param configuredCatalogJson JSON string of configured catalog
     * @param discoveredSchema discovered schema object
     * @return SchemaChange with status and details
     */
    public SchemaChange detectChanges(String configuredCatalogJson,
            DiscoveredSchema discoveredSchema) {
        SchemaChange result = new SchemaChange();
        result.setStatus(SchemaChangeStatus.NO_CHANGE);
        result.setChanges(new ArrayList<>());

        if (StringUtils.isBlank(configuredCatalogJson) || discoveredSchema == null
                || discoveredSchema.getTables() == null) {
            return result;
        }

        JSONObject configured;
        try {
            configured = JSON.parseObject(configuredCatalogJson);
        } catch (Exception e) {
            log.warn("Failed to parse configured catalog JSON", e);
            return result;
        }

        JSONArray configuredStreams = configured.getJSONArray("streams");
        if (configuredStreams == null || configuredStreams.isEmpty()) {
            return result;
        }

        // Build lookup maps
        Map<String, JSONObject> configuredMap = new HashMap<>();
        for (int i = 0; i < configuredStreams.size(); i++) {
            JSONObject stream = configuredStreams.getJSONObject(i);
            String streamName = stream.getString("streamName");
            Boolean selected = stream.getBoolean("selected");
            // Only consider selected streams
            if (streamName != null && !Boolean.FALSE.equals(selected)) {
                configuredMap.put(streamName, stream);
            }
        }

        Map<String, DiscoveredSchema.DiscoveredTable> discoveredMap = new HashMap<>();
        for (DiscoveredSchema.DiscoveredTable table : discoveredSchema.getTables()) {
            discoveredMap.put(table.getTableName(), table);
        }

        boolean hasBreaking = false;
        boolean hasNonBreaking = false;

        // Check for removed tables (BREAKING)
        for (String tableName : configuredMap.keySet()) {
            if (!discoveredMap.containsKey(tableName)) {
                StreamChange change = new StreamChange();
                change.setStreamName(tableName);
                change.setChangeType(ChangeType.REMOVED);
                change.setColumnChanges(new ArrayList<>());
                result.getChanges().add(change);
                hasBreaking = true;
                log.info("Detected BREAKING change: table '{}' removed", tableName);
            }
        }

        // Check for added tables (NON_BREAKING)
        for (String tableName : discoveredMap.keySet()) {
            if (!configuredMap.containsKey(tableName)) {
                StreamChange change = new StreamChange();
                change.setStreamName(tableName);
                change.setChangeType(ChangeType.ADDED);
                change.setColumnChanges(new ArrayList<>());
                result.getChanges().add(change);
                hasNonBreaking = true;
                log.debug("Detected NON_BREAKING change: table '{}' added", tableName);
            }
        }

        // Check column changes for existing tables
        for (String tableName : configuredMap.keySet()) {
            if (discoveredMap.containsKey(tableName)) {
                JSONObject configuredStream = configuredMap.get(tableName);
                DiscoveredSchema.DiscoveredTable discoveredTable = discoveredMap.get(tableName);

                List<ColumnChange> columnChanges =
                        detectColumnChanges(configuredStream, discoveredTable);

                if (!columnChanges.isEmpty()) {
                    StreamChange change = new StreamChange();
                    change.setStreamName(tableName);
                    change.setChangeType(ChangeType.TYPE_CHANGED);
                    change.setColumnChanges(columnChanges);
                    result.getChanges().add(change);

                    // Determine if any column changes are breaking
                    for (ColumnChange cc : columnChanges) {
                        if (cc.getChangeType() == ChangeType.REMOVED
                                || cc.getChangeType() == ChangeType.TYPE_CHANGED
                                || cc.getChangeType() == ChangeType.PRIMARY_KEY_CHANGED) {
                            hasBreaking = true;
                            log.info("Detected BREAKING change in table '{}': column '{}' {}",
                                    tableName, cc.getColumnName(), cc.getChangeType());
                        } else {
                            hasNonBreaking = true;
                        }
                    }
                }
            }
        }

        // Determine overall status
        if (hasBreaking) {
            result.setStatus(SchemaChangeStatus.BREAKING);
        } else if (hasNonBreaking) {
            result.setStatus(SchemaChangeStatus.NON_BREAKING);
        }

        log.info("Schema change detection complete: status={}, changes={}", result.getStatus(),
                result.getChanges().size());
        return result;
    }

    /**
     * Detect column-level changes between a configured stream and discovered table.
     */
    private List<ColumnChange> detectColumnChanges(JSONObject configuredStream,
            DiscoveredSchema.DiscoveredTable discoveredTable) {
        List<ColumnChange> changes = new ArrayList<>();

        // Get configured columns from schema or columns field
        Map<String, ColumnInfo> configuredCols = extractConfiguredColumns(configuredStream);
        Set<String> configuredPrimaryKeys = extractPrimaryKeys(configuredStream);

        // Get discovered columns
        Map<String, ColumnInfo> discoveredCols = new HashMap<>();
        if (discoveredTable.getColumns() != null) {
            for (DiscoveredSchema.DiscoveredColumn col : discoveredTable.getColumns()) {
                discoveredCols.put(col.getColumnName(),
                        new ColumnInfo(col.getColumnType(), col.isNullable()));
            }
        }

        Set<String> discoveredNames = discoveredCols.keySet();

        // Check for removed and changed columns
        for (Map.Entry<String, ColumnInfo> entry : configuredCols.entrySet()) {
            String colName = entry.getKey();
            ColumnInfo configInfo = entry.getValue();

            if (!discoveredNames.contains(colName)) {
                // Column removed (BREAKING)
                ColumnChange change = new ColumnChange();
                change.setColumnName(colName);
                change.setChangeType(ChangeType.REMOVED);
                change.setPreviousType(configInfo.type);
                changes.add(change);
            } else {
                ColumnInfo discInfo = discoveredCols.get(colName);

                // Check type change
                if (!typesCompatible(configInfo.type, discInfo.type)) {
                    ColumnChange change = new ColumnChange();
                    change.setColumnName(colName);
                    change.setChangeType(ChangeType.TYPE_CHANGED);
                    change.setPreviousType(configInfo.type);
                    change.setCurrentType(discInfo.type);
                    changes.add(change);
                }

                // Check nullable change (non-nullable to nullable is breaking for some DBs)
                if (configInfo.nullable != null && discInfo.nullable != null && !configInfo.nullable
                        && discInfo.nullable) {
                    ColumnChange change = new ColumnChange();
                    change.setColumnName(colName);
                    change.setChangeType(ChangeType.NULLABLE_CHANGED);
                    change.setPreviousType("NOT NULL");
                    change.setCurrentType("NULLABLE");
                    changes.add(change);
                }
            }
        }

        // Check for added columns (NON_BREAKING)
        for (String colName : discoveredNames) {
            if (!configuredCols.containsKey(colName)) {
                ColumnChange change = new ColumnChange();
                change.setColumnName(colName);
                change.setChangeType(ChangeType.ADDED);
                change.setCurrentType(discoveredCols.get(colName).type);
                changes.add(change);
            }
        }

        return changes;
    }

    /**
     * Extract column information from configured stream.
     */
    private Map<String, ColumnInfo> extractConfiguredColumns(JSONObject configuredStream) {
        Map<String, ColumnInfo> columns = new HashMap<>();

        // Try schema.properties first (Airbyte format)
        JSONObject schema = configuredStream.getJSONObject("schema");
        if (schema != null) {
            JSONArray properties = schema.getJSONArray("properties");
            if (properties != null) {
                for (int i = 0; i < properties.size(); i++) {
                    JSONObject prop = properties.getJSONObject(i);
                    String name = prop.getString("name");
                    String type = prop.getString("type");
                    Boolean nullable = prop.getBoolean("nullable");
                    if (name != null) {
                        columns.put(name, new ColumnInfo(type, nullable));
                    }
                }
            }
        }

        // Also check columns field (simplified format)
        String columnsStr = configuredStream.getString("columns");
        if (StringUtils.isNotBlank(columnsStr) && !"*".equals(columnsStr)) {
            for (String col : columnsStr.split(",")) {
                String trimmed = col.trim();
                if (StringUtils.isNotBlank(trimmed) && !columns.containsKey(trimmed)) {
                    columns.put(trimmed, new ColumnInfo(null, null));
                }
            }
        }

        return columns;
    }

    /**
     * Extract primary keys from configured stream.
     */
    private Set<String> extractPrimaryKeys(JSONObject configuredStream) {
        Set<String> keys = new HashSet<>();

        // Check primaryKey field
        String pkStr = configuredStream.getString("primaryKey");
        if (StringUtils.isNotBlank(pkStr)) {
            for (String key : pkStr.split(",")) {
                String trimmed = key.trim();
                if (StringUtils.isNotBlank(trimmed)) {
                    keys.add(trimmed);
                }
            }
        }

        // Check primaryKey array
        JSONArray pkArray = configuredStream.getJSONArray("primaryKey");
        if (pkArray != null) {
            for (int i = 0; i < pkArray.size(); i++) {
                String key = pkArray.getString(i);
                if (StringUtils.isNotBlank(key)) {
                    keys.add(key);
                }
            }
        }

        return keys;
    }

    /**
     * Check if two column types are compatible (simplified check).
     */
    private boolean typesCompatible(String type1, String type2) {
        if (type1 == null || type2 == null) {
            return true; // Unknown types are assumed compatible
        }

        String normalizedType1 = normalizeType(type1);
        String normalizedType2 = normalizeType(type2);

        return normalizedType1.equals(normalizedType2);
    }

    /**
     * Normalize column type for comparison.
     */
    private String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        String upper = type.toUpperCase().trim();

        // Remove size specifications like VARCHAR(255) -> VARCHAR
        int parenIndex = upper.indexOf('(');
        if (parenIndex > 0) {
            upper = upper.substring(0, parenIndex);
        }

        // Normalize common type aliases
        switch (upper) {
            case "INT":
            case "INTEGER":
            case "INT4":
                return "INTEGER";
            case "BIGINT":
            case "INT8":
                return "BIGINT";
            case "SMALLINT":
            case "INT2":
                return "SMALLINT";
            case "FLOAT":
            case "FLOAT4":
            case "REAL":
                return "FLOAT";
            case "DOUBLE":
            case "FLOAT8":
            case "DOUBLE PRECISION":
                return "DOUBLE";
            case "DECIMAL":
            case "NUMERIC":
                return "DECIMAL";
            case "VARCHAR":
            case "CHARACTER VARYING":
            case "NVARCHAR":
                return "VARCHAR";
            case "CHAR":
            case "CHARACTER":
            case "NCHAR":
                return "CHAR";
            case "TEXT":
            case "LONGTEXT":
            case "MEDIUMTEXT":
            case "TINYTEXT":
                return "TEXT";
            case "BLOB":
            case "LONGBLOB":
            case "MEDIUMBLOB":
            case "TINYBLOB":
            case "BYTEA":
                return "BLOB";
            case "BOOLEAN":
            case "BOOL":
            case "BIT":
                return "BOOLEAN";
            case "DATE":
                return "DATE";
            case "TIME":
                return "TIME";
            case "DATETIME":
            case "TIMESTAMP":
            case "TIMESTAMP WITHOUT TIME ZONE":
            case "TIMESTAMP WITH TIME ZONE":
                return "TIMESTAMP";
            case "JSON":
            case "JSONB":
                return "JSON";
            default:
                return upper;
        }
    }

    /**
     * Helper class for column information.
     */
    private static class ColumnInfo {
        final String type;
        final Boolean nullable;

        ColumnInfo(String type, Boolean nullable) {
            this.type = type;
            this.nullable = nullable;
        }
    }

    /**
     * Generate a human-readable summary of schema changes.
     */
    public String generateChangeSummary(SchemaChange change) {
        if (change == null || change.getChanges() == null || change.getChanges().isEmpty()) {
            return "No schema changes detected.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Schema change status: ").append(change.getStatus()).append("\n\n");

        int addedTables = 0;
        int removedTables = 0;
        int modifiedTables = 0;
        int totalColumnChanges = 0;

        for (StreamChange sc : change.getChanges()) {
            switch (sc.getChangeType()) {
                case ADDED:
                    addedTables++;
                    break;
                case REMOVED:
                    removedTables++;
                    break;
                default:
                    modifiedTables++;
                    if (sc.getColumnChanges() != null) {
                        totalColumnChanges += sc.getColumnChanges().size();
                    }
                    break;
            }
        }

        if (addedTables > 0) {
            sb.append("- ").append(addedTables).append(" new table(s) available\n");
        }
        if (removedTables > 0) {
            sb.append("- ").append(removedTables).append(" table(s) removed (BREAKING)\n");
        }
        if (modifiedTables > 0) {
            sb.append("- ").append(modifiedTables).append(" table(s) modified with ")
                    .append(totalColumnChanges).append(" column change(s)\n");
        }

        return sb.toString();
    }
}
