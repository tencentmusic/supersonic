package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.knowledge.WordNatureService;
import com.tencent.supersonic.chat.application.query.LLMSemanticQuery;
import com.tencent.supersonic.chat.domain.config.LLMConfig;
import com.tencent.supersonic.chat.domain.pojo.chat.DomainInfos;
import com.tencent.supersonic.chat.domain.pojo.chat.LLMReq;
import com.tencent.supersonic.chat.domain.pojo.chat.LLMResp;
import com.tencent.supersonic.chat.domain.pojo.chat.LLMSchema;
import com.tencent.supersonic.chat.domain.utils.DslToSemanticInfo;
import com.tencent.supersonic.chat.domain.utils.SemanticSatisfactionChecker;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.common.util.json.JsonUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class LLMSemanticParser implements SemanticParser {

    @Override
    public void parse(QueryContextReq queryContext, ChatContext chatCtx) {
        if (SemanticSatisfactionChecker.check(queryContext)) {
            log.info("There is no need parse by llm , queryText:{}", queryContext.getQueryText());
            return;
        }

        Integer domainId = getDomainId(queryContext, chatCtx);
        LLMResp llmResp = requestLLM(queryContext, domainId);
        if (Objects.isNull(llmResp)) {
            return;
        }
        LLMSemanticQuery semanticQuery = new LLMSemanticQuery();
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        String sql = convertToSql(llmResp, parseInfo);
        parseInfo.setDomainId(Long.valueOf(domainId));
        parseInfo.setBonus(queryContext.getQueryText().length() * 1.0);
        parseInfo.setQueryMode(LLMSemanticQuery.QUERY_MODE);
        parseInfo.setInfo(sql);
        queryContext.getCandidateQueries().add(semanticQuery);
        return;
    }

    protected String convertToSql(LLMResp llmResp, SemanticParseInfo parseInfo) {
        return DslToSemanticInfo.convert(parseInfo, llmResp);
    }

    protected LLMResp requestLLM(QueryContextReq queryContext, Integer domainId) {
        try {
            final LLMConfig llmConfig = ContextUtils.getBean(LLMConfig.class);

            DomainInfos domainInfos = ContextUtils.getBean(WordNatureService.class).getCache().getUnchecked("");

            Map<Integer, String> domainIdToName = domainInfos.getDomains().stream()
                    .collect(Collectors.toMap(ItemDO::getDomain, a -> a.getName(), (k1, k2) -> k1));

            Map<Integer, String> itemIdToName = domainInfos.getDimensions().stream()
                    .filter(entry -> domainId.equals(entry.getDomain()))
                    .collect(Collectors.toMap(ItemDO::getItemId, ItemDO::getName, (value1, value2) -> value2));

            String domainName = domainIdToName.get(domainId);
            LLMReq llmReq = new LLMReq();
            llmReq.setQueryText(queryContext.getQueryText());

            List<SchemaElementMatch> matchedElements = queryContext.getMapInfo().getMatchedElements(domainId);

            List<String> fieldNameList = matchedElements.stream()
                    .filter(schemaElementMatch ->
                            SchemaElementType.METRIC.equals(schemaElementMatch.getElementType()) ||
                                    SchemaElementType.DIMENSION.equals(schemaElementMatch.getElementType()) ||
                                    SchemaElementType.VALUE.equals(schemaElementMatch.getElementType()))
                    .map(schemaElementMatch -> {
                        if (!SchemaElementType.VALUE.equals(schemaElementMatch.getElementType())) {
                            return schemaElementMatch.getWord();
                        }
                        return itemIdToName.get(schemaElementMatch.getElementID());
                    })
                    .filter(name -> StringUtils.isNotEmpty(name) && !name.contains("%"))
                    .collect(Collectors.toList());

            LLMSchema llmSchema = new LLMSchema();
            llmSchema.setDomainName(domainName);
            llmSchema.setFieldNameList(fieldNameList);
            llmReq.setSchema(llmSchema);

            log.info("domainId:{},llmReq:{}", domainId, llmReq);
            String questUrl = llmConfig.getUrl() + llmConfig.getQueryToSqlPath();

            RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toString(llmReq), headers);

            log.info("requestLLM request:{},entity:{}", questUrl, entity);
            ResponseEntity<LLMResp> responseEntity = restTemplate.exchange(questUrl, HttpMethod.POST, entity,
                    LLMResp.class);

            log.info("requestLLM result:{}", responseEntity);
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("requestLLM error", e);
        }
        return null;
    }

    protected Integer getDomainId(QueryContextReq queryContext, ChatContext chatCtx) {
        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        Set<Integer> matchedDomains = mapInfo.getMatchedDomains();
        Map<Integer, SemanticQuery> domainQueryModes = new HashMap<>();
        for (Integer matchedDomain : matchedDomains) {
            domainQueryModes.put(matchedDomain, new LLMSemanticQuery());
        }
        List<DomainResolver> domainResolverList = SpringFactoriesLoader.loadFactories(DomainResolver.class,
                Thread.currentThread().getContextClassLoader());
        Optional<Integer> domainId = domainResolverList.stream()
                .map(domainResolver -> domainResolver.resolve(domainQueryModes, queryContext, chatCtx,
                        queryContext.getMapInfo())).filter(d -> d > 0).findFirst();
        if (domainId.isPresent()) {
            return domainId.get();
        }
        return 0;
    }
}


