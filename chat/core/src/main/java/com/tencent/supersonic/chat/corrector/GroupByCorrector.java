package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class GroupByCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {

        super.correct(semanticCorrectInfo);

        //add aggregate to all metric
        String sql = semanticCorrectInfo.getSql();
        Long modelId = semanticCorrectInfo.getParseInfo().getModel().getModel();

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        Set<String> dimensions = semanticSchema.getDimensions(modelId).stream()
                .filter(schemaElement -> !DateUtils.DATE_FIELD.equals(schemaElement.getBizName()))
                .map(schemaElement -> schemaElement.getName()).collect(Collectors.toSet());

        List<String> selectFields = SqlParserSelectHelper.getSelectFields(sql);

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        Set<String> groupByFields = selectFields.stream().filter(field -> dimensions.contains(field))
                .collect(Collectors.toSet());
        semanticCorrectInfo.setSql(SqlParserUpdateHelper.addGroupBy(sql, groupByFields));
    }
}
