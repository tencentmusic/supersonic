package com.tencent.supersonic.headless.core.translator.parser.calcite.render;

import com.tencent.supersonic.headless.core.pojo.DataModel;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.calcite.TableView;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.SemanticNode;
import lombok.Data;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.List;

/** process TableView */
@Data
public abstract class Renderer {

    protected TableView tableView = new TableView();

    public void setTable(SqlNode table) {
        tableView.setTable(table);
    }

    public SqlNode build() {
        return tableView.build();
    }

    public SqlNode builderAs(String alias) throws Exception {
        return SemanticNode.buildAs(alias, tableView.build());
    }

    public abstract void render(OntologyQuery ontologyQuery, List<DataModel> dataModels,
            SqlValidatorScope scope, S2CalciteSchema schema, boolean nonAgg) throws Exception;
}
