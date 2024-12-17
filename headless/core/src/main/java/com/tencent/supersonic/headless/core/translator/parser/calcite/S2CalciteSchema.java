package com.tencent.supersonic.headless.core.translator.parser.calcite;

import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.core.pojo.JoinRelation;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.translator.parser.RuntimeOptions;
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

    public Map<String, ModelResp> getDataModels() {
        return ontology.getModelMap();
    }

    public List<MetricSchemaResp> getMetrics() {
        return ontology.getMetrics();
    }

    public Map<String, List<DimSchemaResp>> getDimensions() {
        return ontology.getDimensionMap();
    }

    public List<JoinRelation> getJoinRelations() {
        return ontology.getJoinRelations();
    }

}
