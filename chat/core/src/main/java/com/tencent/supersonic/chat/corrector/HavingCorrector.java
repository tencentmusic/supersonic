package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
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
    public void work(QueryReq queryReq, SemanticParseInfo semanticParseInfo) {

        //add aggregate to all metric
        Long modelId = semanticParseInfo.getModel().getModel();

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        Set<String> metrics = semanticSchema.getMetrics(modelId).stream()
                .map(schemaElement -> schemaElement.getName()).collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(metrics)) {
            return;
        }
        String havingSql = SqlParserAddHelper.addHaving(semanticParseInfo.getSqlInfo().getLogicSql(), metrics);
        semanticParseInfo.getSqlInfo().setLogicSql(havingSql);
    }

}
