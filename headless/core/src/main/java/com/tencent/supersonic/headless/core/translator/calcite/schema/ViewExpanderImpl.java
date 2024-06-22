package com.tencent.supersonic.headless.core.translator.calcite.schema;

import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;

import java.util.List;

/**
 * customize the  ViewExpander
 */
public class ViewExpanderImpl implements RelOptTable.ViewExpander {
    public ViewExpanderImpl() {
    }

    @Override
    public RelRoot expandView(RelDataType rowType, String queryString, List<String> schemaPath,
            List<String> dataSetPath) {
        return null;
    }
}