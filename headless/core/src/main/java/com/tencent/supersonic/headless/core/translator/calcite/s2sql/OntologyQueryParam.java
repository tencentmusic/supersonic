package com.tencent.supersonic.headless.core.translator.calcite.s2sql;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import lombok.Data;

import java.util.List;

@Data
public class OntologyQueryParam {
    private List<String> metrics = Lists.newArrayList();
    private List<String> dimensions = Lists.newArrayList();
    private String where;
    private Long limit;
    private List<ColumnOrder> order;
    private boolean nativeQuery = false;
    private AggOption aggOption = AggOption.DEFAULT;
}
