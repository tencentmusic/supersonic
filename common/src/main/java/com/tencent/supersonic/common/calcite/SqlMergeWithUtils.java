package com.tencent.supersonic.common.calcite;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SqlMergeWithUtils {
    public static String mergeWith(EngineType engineType, String sql, List<String> parentSqlList,
            List<String> parentWithNameList) throws Exception {

        Select selectStatement = (Select) CCJSqlParserUtil.parse(sql);
        List<WithItem> withItemList = new ArrayList<>();

        for (int i = 0; i < parentSqlList.size(); i++) {
            String parentSql = parentSqlList.get(i);
            String parentWithName = parentWithNameList.get(i);

            Select parentSelect = (Select) CCJSqlParserUtil.parse(parentSql);
            ParenthesedSelect select = new ParenthesedSelect();
            select.setSelect(parentSelect);

            // Create a new WITH item for parentWithName without quotes
            WithItem withItem = new WithItem();
            withItem.setAlias(new Alias(parentWithName));
            withItem.setSelect(select);

            // Add the new WITH item to the list
            withItemList.add(withItem);
        }

        // Extract existing WITH items from mainSelectBody if it has any
        if (selectStatement.getWithItemsList() != null) {
            withItemList.addAll(selectStatement.getWithItemsList());
        }

        // Set the new WITH items list to the main select body
        selectStatement.setWithItemsList(withItemList);

        // Pretty print the final SQL
        return selectStatement.toString();
    }
}
