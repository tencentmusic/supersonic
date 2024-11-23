package com.tencent.supersonic.headless.core.translator.calcite.sql;

import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataModel;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.JoinRelation;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Ontology;
import lombok.Builder;
import lombok.Data;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaVersion;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class S2CalciteSchema extends AbstractSchema {

    private String schemaKey;

    private Ontology ontology;

    private RuntimeOptions runtimeOptions;

    @Override
    public Schema snapshot(SchemaVersion version) {
        return this;
    }

    public Map<String, DataModel> getDataModels() {
        return ontology.getDataModelMap();
    }

    public List<Metric> getMetrics() {
        return ontology.getMetrics();
    }

    public Map<String, List<Dimension>> getDimensions() {
        return ontology.getDimensionMap();
    }

    public List<JoinRelation> getJoinRelations() {
        return ontology.getJoinRelations();
    }

}
