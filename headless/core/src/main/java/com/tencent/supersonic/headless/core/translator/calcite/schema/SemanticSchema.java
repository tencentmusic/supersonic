package com.tencent.supersonic.headless.core.translator.calcite.schema;


import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Materialization;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.JoinRelation;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaVersion;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SemanticSchema extends AbstractSchema {

    private final String schemaKey;
    private final Map<String, Table> tableMap;

    private SemanticModel semanticModel = new SemanticModel();

    private List<JoinRelation> joinRelations;

    private RuntimeOptions runtimeOptions;


    private SemanticSchema(String schemaKey, Map<String, Table> tableMap) {
        this.schemaKey = schemaKey;
        this.tableMap = tableMap;
    }

    public static Builder newBuilder(String schemaKey) {
        return new Builder(schemaKey);
    }

    public String getSchemaKey() {
        return schemaKey;
    }

    public void setSemanticModel(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
    }

    public SemanticModel getSemanticModel() {
        return semanticModel;
    }

    @Override
    public Map<String, Table> getTableMap() {
        return tableMap;
    }

    @Override
    public Schema snapshot(SchemaVersion version) {
        return this;
    }

    public Map<String, DataSource> getDatasource() {
        return semanticModel.getDatasourceMap();
    }

    public void setDatasource(Map<String, DataSource> datasource) {
        semanticModel.setDatasourceMap(datasource);
    }

    public Map<String, List<Dimension>> getDimension() {
        return semanticModel.getDimensionMap();
    }

    public void setDimension(Map<String, List<Dimension>> dimensions) {
        semanticModel.setDimensionMap(dimensions);
    }

    public List<Metric> getMetrics() {
        return semanticModel.getMetrics();
    }

    public void setMetric(List<Metric> metric) {
        semanticModel.setMetrics(metric);
    }

    public void setMaterializationList(List<Materialization> materializationList) {
        semanticModel.setMaterializationList(materializationList);
    }

    public List<Materialization> getMaterializationList() {
        return semanticModel.getMaterializationList();
    }

    public void setJoinRelations(List<JoinRelation> joinRelations) {
        this.joinRelations = joinRelations;
    }

    public List<JoinRelation> getJoinRelations() {
        return joinRelations;
    }

    public void setRuntimeOptions(RuntimeOptions runtimeOptions) {
        this.runtimeOptions = runtimeOptions;
    }

    public RuntimeOptions getRuntimeOptions() {
        return runtimeOptions;
    }

    public static final class Builder {

        private final String schemaKey;
        private final Map<String, Table> tableMap = new HashMap<>();

        private Builder(String schemaKey) {
            if (schemaKey == null) {
                throw new IllegalArgumentException("Schema name cannot be null or empty");
            }

            this.schemaKey = schemaKey;
        }

        public Builder addTable(DataSourceTable table) {
            if (tableMap.containsKey(table.getTableName())) {
                throw new IllegalArgumentException("Table already defined: " + table.getTableName());
            }

            tableMap.put(table.getTableName(), table);

            return this;
        }

        public SemanticSchema build() {
            return new SemanticSchema(schemaKey, tableMap);
        }
    }

}
