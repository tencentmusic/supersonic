package com.tencent.supersonic.headless.core.translator.calcite.sql.node;

import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Identify;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class IdentifyNode extends SemanticNode {

    public static SqlNode build(Identify identify, SqlValidatorScope scope, EngineType engineType) throws Exception {
        return parse(identify.getName(), scope, engineType);
    }

    public static Set<String> getIdentifyNames(List<Identify> identifies, Identify.Type type) {
        return identifies.stream().filter(i -> type.name().equalsIgnoreCase(i.getType())).map(i -> i.getName())
                .collect(Collectors.toSet());

    }

    public static boolean isForeign(String name, List<Identify> identifies) {
        Optional<Identify> identify = identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name))
                .findFirst();
        if (identify.isPresent()) {
            return Identify.Type.FOREIGN.name().equalsIgnoreCase(identify.get().getType());
        }
        return false;
    }

    public static boolean isPrimary(String name, List<Identify> identifies) {
        Optional<Identify> identify = identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name))
                .findFirst();
        if (identify.isPresent()) {
            return Identify.Type.PRIMARY.name().equalsIgnoreCase(identify.get().getType());
        }
        return false;
    }
}
