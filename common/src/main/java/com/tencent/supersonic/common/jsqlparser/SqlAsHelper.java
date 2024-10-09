package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.WithItem;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SqlAsHelper {

    private static void extractAliasesFromSelect(PlainSelect plainSelect, Set<String> aliases) {
        // Extract aliases from SELECT items
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            Alias alias = selectItem.getAlias();
            if (alias != null) {
                aliases.add(alias.getName());
            }
        }
        FunctionAliasVisitor visitor = new FunctionAliasVisitor(aliases);
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(visitor);
        }
    }


    public static List<String> getAsFields(String sql) {
        List<PlainSelect> plainSelectList = SqlSelectHelper.getPlainSelect(sql);
        if (CollectionUtils.isEmpty(plainSelectList)) {
            return new ArrayList<>();
        }
        Set<String> aliases = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (plainSelect instanceof Select) {
                Select select = plainSelect;
                Select selectBody = select.getSelectBody();
                selectBody.accept(new SelectVisitorAdapter() {
                    @Override
                    public void visit(PlainSelect plainSelect) {
                        extractAliasesFromSelect(plainSelect, aliases);
                    }

                    @Override
                    public void visit(WithItem withItem) {
                        withItem.getSelectBody().accept(this);
                    }
                });
            }
        }
        return new ArrayList<>(aliases);
    }

    public static Map<String, String> getFieldMapFilterByAsFields(String sql,
            Map<String, String> fieldNameMap) {
        // Delete aliases if they exist
        List<String> asFields = SqlAsHelper.getAsFields(sql);
        Set<String> asFieldsSet = new HashSet<>(asFields);
        fieldNameMap = fieldNameMap.entrySet().stream()
                .filter(entry -> !asFieldsSet.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fieldNameMap;
    }
}
