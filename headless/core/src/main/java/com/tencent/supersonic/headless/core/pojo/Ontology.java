package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.DataModel;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Dimension;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Materialization;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Metric;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class Ontology {

    private List<Metric> metrics = new ArrayList<>();
    private Map<String, DataModel> dataModelMap = new HashMap<>();
    private Map<String, List<Dimension>> dimensionMap = new HashMap<>();
    private List<Materialization> materializationList = new ArrayList<>();
    private List<JoinRelation> joinRelations;
    private DatabaseResp database;

    public List<Dimension> getDimensions() {
        return dimensionMap.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public EngineType getDatabaseType() {
        if (Objects.nonNull(database)) {
            return EngineType.fromString(database.getType().toUpperCase());
        }
        return null;
    }

    public String getDatabaseVersion() {
        if (Objects.nonNull(database)) {
            return database.getVersion();
        }
        return null;
    }
}
