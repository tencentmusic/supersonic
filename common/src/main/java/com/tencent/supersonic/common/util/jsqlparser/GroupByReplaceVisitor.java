package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Map;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;
import org.apache.commons.lang3.StringUtils;

public class GroupByReplaceVisitor implements GroupByVisitor {

    ParseVisitorHelper parseVisitorHelper = new ParseVisitorHelper();
    private Map<String, String> fieldToBizName;


    public GroupByReplaceVisitor(Map<String, String> fieldToBizName) {
        this.fieldToBizName = fieldToBizName;
    }

    public void visit(GroupByElement groupByElement) {
        groupByElement.getGroupByExpressionList();
        ExpressionList groupByExpressionList = groupByElement.getGroupByExpressionList();
        List<Expression> groupByExpressions = groupByExpressionList.getExpressions();

        for (int i = 0; i < groupByExpressions.size(); i++) {
            Expression expression = groupByExpressions.get(i);

            String replaceColumn = parseVisitorHelper.getReplaceColumn(expression.toString(), fieldToBizName);
            if (StringUtils.isNotEmpty(replaceColumn)) {
                groupByExpressions.set(i, new Column(replaceColumn));
            }
        }
    }
}