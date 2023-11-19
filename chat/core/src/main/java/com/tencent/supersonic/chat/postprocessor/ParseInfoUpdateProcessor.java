package com.tencent.supersonic.chat.postprocessor;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.service.ParseInfoService;
import com.tencent.supersonic.common.util.ContextUtils;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * update parse info from correct sql
 */
public class ParseInfoUpdateProcessor implements PostProcessor {

    @Override
    public void process(QueryContext queryContext) {
        List<SemanticQuery> candidateQueries = queryContext.getCandidateQueries();
        if (CollectionUtils.isEmpty(candidateQueries)) {
            return;
        }
        ParseInfoService parseInfoService = ContextUtils.getBean(ParseInfoService.class);
        List<SemanticParseInfo> candidateParses = candidateQueries.stream()
                .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
        candidateParses.forEach(parseInfoService::updateParseInfo);
    }

}