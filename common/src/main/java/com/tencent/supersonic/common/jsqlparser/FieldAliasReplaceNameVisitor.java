package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class FieldAliasReplaceNameVisitor extends SelectItemVisitorAdapter {
    private Map<String, String> fieldNameMap;

    private Map<String, String> aliasToActualExpression = new HashMap<>();

    public FieldAliasReplaceNameVisitor(Map<String, String> fieldNameMap) {
        this.fieldNameMap = fieldNameMap;
    }

    @Override
    public void visit(SelectItem selectExpressionItem) {
        Alias alias = selectExpressionItem.getAlias();
        if (alias == null) {
            return;
        }
        String aliasName = alias.getName();
        String replaceValue = fieldNameMap.get(aliasName);
        if (StringUtils.isBlank(replaceValue)) {
            return;
        }

        aliasToActualExpression.put(aliasName, replaceValue);
        alias.setName(replaceValue);
    }

    public Map<String, String> getAliasToActualExpression() {
        return aliasToActualExpression;
    }
}
