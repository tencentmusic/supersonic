package com.tencent.supersonic.headless.core.parser.calcite.s2sql;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.headless.api.response.DatabaseResp;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class HeadlessModel {

    private String rootPath;
    private List<Metric> metrics = new ArrayList<>();
    private Map<String, DataSource> datasourceMap = new HashMap<>();
    private Map<String, List<Dimension>> dimensionMap = new HashMap<>();
    private List<Materialization> materializationList = new ArrayList<>();
    private List<JoinRelation> joinRelations;
    private ItemDateResp dataDate;
    private DatabaseResp databaseResp;

    public List<Dimension> getDimensions() {
        return dimensionMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public Map<Long, DataSource> getModelMap() {
        return datasourceMap.values().stream().collect(Collectors.toMap(DataSource::getId, dataSource -> dataSource));
    }

}
