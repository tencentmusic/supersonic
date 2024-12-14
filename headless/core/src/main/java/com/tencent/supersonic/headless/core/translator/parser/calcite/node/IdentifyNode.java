package com.tencent.supersonic.headless.core.translator.parser.calcite.node;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.List;
import java.util.Optional;

public class IdentifyNode extends SemanticNode {

    public static SqlNode build(Identify identify, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        return parse(identify.getName(), scope, engineType);
    }

    public static boolean isForeign(String name, List<Identify> identifies) {
        Optional<Identify> identify =
                identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return IdentifyType.foreign.equals(identify.get().getType());
        }
        return false;
    }

    public static boolean isPrimary(String name, List<Identify> identifies) {
        Optional<Identify> identify =
                identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return IdentifyType.primary.equals(identify.get().getType());
        }
        return false;
    }
}
