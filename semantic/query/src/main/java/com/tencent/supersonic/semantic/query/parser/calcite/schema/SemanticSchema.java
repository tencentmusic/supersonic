package com.tencent.supersonic.semantic.query.parser.calcite.schema;


import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Dimension;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Materialization;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Metric;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.SemanticModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaVersion;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

public class SemanticSchema extends AbstractSchema {

    private final String rootPath;
    private final Map<String, Table> tableMap;

    private SemanticModel semanticModel = new SemanticModel();


    private SemanticSchema(String rootPath, Map<String, Table> tableMap) {
        this.rootPath = rootPath;
        this.tableMap = tableMap;
    }

    public static Builder newBuilder(String rootPath) {
        return new Builder(rootPath);
    }

    public String getRootPath() {
        return rootPath;
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


        public SemanticSchema build() {
            return new SemanticSchema(rootPath, tableMap);
        }
    }
}