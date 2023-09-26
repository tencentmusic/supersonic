package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public abstract class BaseSemanticCorrector implements SemanticCorrector {

    public static final String DATE_FIELD = "数据日期";


    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        semanticCorrectInfo.setPreSql(semanticCorrectInfo.getSql());
    }

    protected Map<String, String> getFieldToBizName(Long modelId) {

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        List<SchemaElement> dbAllFields = new ArrayList<>();
        dbAllFields.addAll(semanticSchema.getMetrics());
        dbAllFields.addAll(semanticSchema.getDimensions());

        Map<String, String> result = dbAllFields.stream()
                .filter(entry -> entry.getModel().equals(modelId))
                .collect(Collectors.toMap(SchemaElement::getName, a -> a.getBizName(), (k1, k2) -> k1));
        result.put(DATE_FIELD, TimeDimensionEnum.DAY.getName());
        return result;
    }

    protected void addFieldsToSelect(SemanticCorrectInfo semanticCorrectInfo, String sql) {
        Set<String> selectFields = new HashSet<>(SqlParserSelectHelper.getSelectFields(sql));
        Set<String> whereFields = new HashSet<>(SqlParserSelectHelper.getWhereFields(sql));

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(whereFields)) {
            return;
        }

        whereFields.addAll(SqlParserSelectHelper.getOrderByFields(sql));
        whereFields.removeAll(selectFields);
        whereFields.remove(TimeDimensionEnum.DAY.getName());
        whereFields.remove(TimeDimensionEnum.WEEK.getName());
        whereFields.remove(TimeDimensionEnum.MONTH.getName());
        String replaceFields = SqlParserUpdateHelper.addFieldsToSelect(sql, new ArrayList<>(whereFields));
        semanticCorrectInfo.setSql(replaceFields);
    }
}
