package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.Set;

public class AliasAcquireVisitor extends ExpressionVisitorAdapter {

    private Set<String> aliases;

    public AliasAcquireVisitor(Set<String> aliases) {
        this.aliases = aliases;
    }

    @Override
    public void visit(SelectItem selectItem) {
        Alias alias = selectItem.getAlias();
        if (alias != null) {
            aliases.add(alias.getName());
        }
    }
}
