package com.tencent.supersonic.semantic.query.parser.calcite.sql.node;

import com.tencent.supersonic.semantic.query.parser.calcite.s2ql.Identify;
import com.tencent.supersonic.semantic.query.parser.calcite.s2ql.Identify.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class IdentifyNode extends SemanticNode {

    public static SqlNode build(Identify identify, SqlValidatorScope scope) throws Exception {
        return parse(identify.getName(), scope);
    }

    public static Set<String> getIdentifyNames(List<Identify> identifies, Identify.Type type) {
        return identifies.stream().filter(i -> type.name().equalsIgnoreCase(i.getType())).map(i -> i.getName())
                .collect(Collectors.toSet());

    }

    public static boolean isForeign(String name, List<Identify> identifies) {
        Optional<Identify> identify = identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name))
                .findFirst();
        if (identify.isPresent()) {
            return Type.FOREIGN.name().equalsIgnoreCase(identify.get().getType());
        }
        return false;
    }

    public static boolean isPrimary(String name, List<Identify> identifies) {
        Optional<Identify> identify = identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name))
                .findFirst();
        if (identify.isPresent()) {
            return Type.PRIMARY.name().equalsIgnoreCase(identify.get().getType());
        }
        return false;
    }
}
