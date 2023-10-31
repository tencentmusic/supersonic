package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class HavingCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {

        super.correct(semanticCorrectInfo);

        //add aggregate to all metric
        semanticCorrectInfo.setPreSql(semanticCorrectInfo.getSql());
        Long modelId = semanticCorrectInfo.getParseInfo().getModel().getModel();

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        Set<String> metrics = semanticSchema.getMetrics(modelId).stream()
                .map(schemaElement -> schemaElement.getName()).collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(metrics)) {
            return;
        }
        String havingSql = SqlParserAddHelper.addHaving(semanticCorrectInfo.getSql(), metrics);
        semanticCorrectInfo.setSql(havingSql);
    }

}
