package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.util.EditDistanceUtils;
import com.tencent.supersonic.common.util.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Data
public class ReplaceService {

    @Value("${s2.replace.threshold:0.4}")
    private double replaceColumnThreshold;

    public void replaceFunction(Function expression, Map<String, String> fieldNameMap,
            boolean exactReplace) {
        Function function = expression;
        ExpressionList<?> expressions = function.getParameters();
        for (Expression column : expressions) {
            if (column instanceof Column) {
                replaceColumn((Column) column, fieldNameMap, exactReplace);
            }
        }
    }

    public void replaceColumn(Column column, Map<String, String> fieldNameMap,
            boolean exactReplace) {
        String columnName = StringUtil.replaceBackticks(column.getColumnName());
        String replaceColumn = getReplaceValue(columnName, fieldNameMap, exactReplace);
        if (StringUtils.isNotBlank(replaceColumn)) {
            column.setColumnName(replaceColumn);
        }
    }

    public String getReplaceValue(String beforeValue, Map<String, String> valueMap,
            boolean exactReplace) {
        String replaceValue = valueMap.get(beforeValue);
        if (StringUtils.isNotBlank(replaceValue)) {
            return replaceValue;
        }
        if (exactReplace) {
            return null;
        }
        Optional<Entry<String, String>> first = valueMap.entrySet().stream().sorted((k1, k2) -> {
            String k1Value = k1.getKey();
            String k2Value = k2.getKey();
            Double k1Similarity = EditDistanceUtils.getSimilarity(beforeValue, k1Value);
            Double k2Similarity = EditDistanceUtils.getSimilarity(beforeValue, k2Value);
            return k2Similarity.compareTo(k1Similarity);
        }).collect(Collectors.toList()).stream().findFirst();

        if (first.isPresent()) {
            replaceValue = first.get().getValue();
            double similarity = EditDistanceUtils.getSimilarity(beforeValue, replaceValue);
            if (similarity > replaceColumnThreshold) {
                return replaceValue;
            }
        }
        return beforeValue;
    }
}
