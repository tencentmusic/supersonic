package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class FieldAliasReplaceWithBackticksVisitor extends SelectItemVisitorAdapter {

    private Map<String, String> fieldAliasReplacedMap = new HashMap<>();

    @Override
    public void visit(SelectItem selectExpressionItem) {
        Alias alias = selectExpressionItem.getAlias();
        if (alias == null) {
            return;
        }
        String aliasName = alias.getName();
        String replaceValue = addBackticks(aliasName);
        if (StringUtils.isBlank(replaceValue)) {
            return;
        }
        alias.setName(replaceValue);
        fieldAliasReplacedMap.put(aliasName, replaceValue);
    }

    private String addBackticks(String aliasName) {
        if (StringUtils.isBlank(aliasName)) {
            return "";
        }
        if (aliasName.startsWith("`") && aliasName.endsWith("`")) {
            return "";
        }
        return "`" + aliasName + "`";
    }

    public Map<String, String> getFieldAliasReplacedMap() {
        return fieldAliasReplacedMap;
    }
}
