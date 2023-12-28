package com.tencent.supersonic.headless.query.parser.calcite.schema;


import com.tencent.supersonic.headless.query.parser.calcite.s2sql.Materialization;
import com.tencent.supersonic.headless.query.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.query.parser.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.query.parser.calcite.s2sql.JoinRelation;
import com.tencent.supersonic.headless.query.parser.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.query.parser.calcite.s2sql.HeadlessModel;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaVersion;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadlessSchema extends AbstractSchema {

    private final String rootPath;
    private final Map<String, Table> tableMap;

    private HeadlessModel headlessModel = new HeadlessModel();

    private List<JoinRelation> joinRelations;

    private RuntimeOptions runtimeOptions;


    private HeadlessSchema(String rootPath, Map<String, Table> tableMap) {
        this.rootPath = rootPath;
        this.tableMap = tableMap;
    }

    public static Builder newBuilder(String rootPath) {
        return new Builder(rootPath);
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setSemanticModel(HeadlessModel headlessModel) {
        this.headlessModel = headlessModel;
    }

    public HeadlessModel getSemanticModel() {
        return headlessModel;
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
        return headlessModel.getDatasourceMap();
    }

    public void setDatasource(Map<String, DataSource> datasource) {
        headlessModel.setDatasourceMap(datasource);
    }

    public Map<String, List<Dimension>> getDimension() {
        return headlessModel.getDimensionMap();
    }

    public void setDimension(Map<String, List<Dimension>> dimensions) {
        headlessModel.setDimensionMap(dimensions);
    }

    public List<Metric> getMetrics() {
        return headlessModel.getMetrics();
    }

    public void setMetric(List<Metric> metric) {
        headlessModel.setMetrics(metric);
    }

    public void setMaterializationList(List<Materialization> materializationList) {
        headlessModel.setMaterializationList(materializationList);
    }

    public List<Materialization> getMaterializationList() {
        return headlessModel.getMaterializationList();
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

        private final String rootPath;
        private final Map<String, Table> tableMap = new HashMap<>();

        private Builder(String rootPath) {
            if (rootPath == null || rootPath.isEmpty()) {
                throw new IllegalArgumentException("Schema name cannot be null or empty");
            }

            this.rootPath = rootPath;
        }

        public Builder addTable(DataSourceTable table) {
            if (tableMap.containsKey(table.getTableName())) {
                throw new IllegalArgumentException("Table already defined: " + table.getTableName());
            }

            tableMap.put(table.getTableName(), table);

            return this;
        }

        public HeadlessSchema build() {
            return new HeadlessSchema(rootPath, tableMap);
        }
    }

}
