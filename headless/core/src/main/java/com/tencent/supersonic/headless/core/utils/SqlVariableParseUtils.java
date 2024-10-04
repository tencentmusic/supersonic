package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.headless.api.pojo.Param;
import com.tencent.supersonic.headless.api.pojo.SqlVariable;
import com.tencent.supersonic.headless.api.pojo.enums.VariableValueType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.COMMA;
import static com.tencent.supersonic.common.pojo.Constants.EMPTY;

@Slf4j
public class SqlVariableParseUtils {

    public static final String REG_SENSITIVE_SQL =
            "drop\\s|alter\\s|grant\\s|insert\\s|replace\\s|delete\\s|"
                    + "truncate\\s|update\\s|remove\\s";
    public static final Pattern PATTERN_SENSITIVE_SQL = Pattern.compile(REG_SENSITIVE_SQL);

    public static final String APOSTROPHE = "'";

    private static final char delimiter = '$';

    public static String parse(String sql, List<SqlVariable> sqlVariables, List<Param> params) {
        Map<String, Object> variables = new HashMap<>();
        if (CollectionUtils.isEmpty(sqlVariables)) {
            return sql;
        }
        // 1. handle default variable value
        sqlVariables.forEach(variable -> {
            variables.put(variable.getName().trim(),
                    getValues(variable.getValueType(), variable.getDefaultValues()));
        });

        // override by variable param
        if (!CollectionUtils.isEmpty(params)) {
            Map<String, List<SqlVariable>> map =
                    sqlVariables.stream().collect(Collectors.groupingBy(SqlVariable::getName));
            params.forEach(p -> {
                if (map.containsKey(p.getName())) {
                    List<SqlVariable> list = map.get(p.getName());
                    if (!CollectionUtils.isEmpty(list)) {
                        SqlVariable v = list.get(list.size() - 1);
                        variables.put(p.getName().trim(), getValue(v.getValueType(), p.getValue()));
                    }
                }
            });
        }

        variables.forEach((k, v) -> {
            if (v instanceof List && ((List) v).size() > 0) {
                v = ((List) v).stream().collect(Collectors.joining(COMMA)).toString();
            }
            variables.put(k, v);
        });
        return parse(sql, variables);
    }

    public static String parse(String sql, Map<String, Object> variables) {
        ST st = new ST(sql, delimiter, delimiter);
        if (!CollectionUtils.isEmpty(variables)) {
            variables.forEach(st::add);
        }
        return st.render();
    }

    public static List<String> getValues(VariableValueType valueType, List<Object> values) {
        if (CollectionUtils.isEmpty(values)) {
            return new ArrayList<>();
        }
        if (null != valueType) {
            switch (valueType) {
                case STRING:
                    return values.stream().map(String::valueOf)
                            .map(s -> s.startsWith(APOSTROPHE) && s.endsWith(APOSTROPHE) ? s
                                    : String.join(EMPTY, APOSTROPHE, s, APOSTROPHE))
                            .collect(Collectors.toList());
                case EXPR:
                    values.stream().map(String::valueOf)
                            .forEach(SqlVariableParseUtils::checkSensitiveSql);
                    return values.stream().map(String::valueOf).collect(Collectors.toList());
                case NUMBER:
                    return values.stream().map(String::valueOf).collect(Collectors.toList());
                default:
                    return values.stream().map(String::valueOf).collect(Collectors.toList());
            }
        }
        return values.stream().map(String::valueOf).collect(Collectors.toList());
    }

    public static Object getValue(VariableValueType valueType, String value) {
        if (!StringUtils.isEmpty(value)) {
            if (null != valueType) {
                switch (valueType) {
                    case STRING:
                        return String.join(EMPTY, value.startsWith(APOSTROPHE) ? EMPTY : APOSTROPHE,
                                value, value.endsWith(APOSTROPHE) ? EMPTY : APOSTROPHE);
                    case NUMBER:
                    case EXPR:
                    default:
                        return value;
                }
            }
        }
        return value;
    }

    public static void checkSensitiveSql(String sql) {
        Matcher matcher = PATTERN_SENSITIVE_SQL.matcher(sql.toLowerCase());
        if (matcher.find()) {
            String group = matcher.group();
            log.warn("Sensitive SQL operations are not allowed: {}", group.toUpperCase());
            throw new InvalidArgumentException(
                    "Sensitive SQL operations are not allowed: " + group.toUpperCase());
        }
    }
}
