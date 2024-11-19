package com.tencent.supersonic.headless.core.translator.calcite.schema;

import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataModel;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.JoinRelation;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Materialization;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Ontology;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaVersion;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S2SemanticSchema extends AbstractSchema {

    private final String schemaKey;

    private final Map<String, Table> tableMap;

    private Ontology ontology = new Ontology();

    private List<JoinRelation> joinRelations;

    private RuntimeOptions runtimeOptions;

    private S2SemanticSchema(String schemaKey, Map<String, Table> tableMap) {
        this.schemaKey = schemaKey;
        this.tableMap = tableMap;
    }

    public static Builder newBuilder(String schemaKey) {
        return new Builder(schemaKey);
    }

    public String getSchemaKey() {
        return schemaKey;
    }

    public void setSemanticModel(Ontology ontology) {
        this.ontology = ontology;
    }

    public Ontology getSemanticModel() {
        return ontology;
    }

    @Override
    public Map<String, Table> getTableMap() {
        return tableMap;
    }

    @Override
    public Schema snapshot(SchemaVersion version) {
        return this;
    }

    public Map<String, DataModel> getDatasource() {
        return ontology.getDatasourceMap();
    }

    public void setDatasource(Map<String, DataModel> datasource) {
        ontology.setDatasourceMap(datasource);
    }

    public Map<String, List<Dimension>> getDimension() {
        return ontology.getDimensionMap();
    }

    public void setDimension(Map<String, List<Dimension>> dimensions) {
        ontology.setDimensionMap(dimensions);
    }

    public List<Metric> getMetrics() {
        return ontology.getMetrics();
    }

    public void setMetric(List<Metric> metric) {
        ontology.setMetrics(metric);
    }

    public void setMaterializationList(List<Materialization> materializationList) {
        ontology.setMaterializationList(materializationList);
    }

    public List<Materialization> getMaterializationList() {
        return ontology.getMaterializationList();
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
                throw new IllegalArgumentException(
                        "Table already defined: " + table.getTableName());
            }

            tableMap.put(table.getTableName(), table);

            return this;
        }

        public S2SemanticSchema build() {
            return new S2SemanticSchema(schemaKey, tableMap);
        }
    }
}
