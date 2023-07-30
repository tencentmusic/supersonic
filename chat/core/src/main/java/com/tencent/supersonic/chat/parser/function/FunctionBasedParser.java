package com.tencent.supersonic.chat.parser.function;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.config.FunctionCallConfig;
import com.tencent.supersonic.chat.parser.ParseMode;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.plugin.dsl.DSLQuery;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class FunctionBasedParser implements SemanticParser {

    public static final double FUNCTION_BONUS_THRESHOLD = 200;

    @Override
    public void parse(QueryContext queryCtx, ChatContext chatCtx) {
        FunctionCallConfig functionCallConfig = ContextUtils.getBean(FunctionCallConfig.class);
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        String functionUrl = functionCallConfig.getUrl();

        if (StringUtils.isBlank(functionUrl) || SatisfactionChecker.check(queryCtx)) {
            log.info("functionUrl:{}, skip function parser, queryText:{}", functionUrl,
                    queryCtx.getRequest().getQueryText());
            return;
        }
        DomainResolver domainResolver = ComponentFactory.getDomainResolver();
        Long domainId = domainResolver.resolve(queryCtx, chatCtx);
        List<String> functionNames = getFunctionNames(domainId);
        log.info("domainId:{},functionNames:{}", domainId, functionNames);
        if (Objects.isNull(domainId) || domainId <= 0) {
            return;
        }
        FunctionReq functionReq = FunctionReq.builder()
                .queryText(queryCtx.getRequest().getQueryText())
                .functionNames(functionNames).build();

        FunctionResp functionResp = requestFunction(functionUrl, functionReq);
        log.info("requestFunction result:{}", functionResp.getToolSelection());
        if (Objects.isNull(functionResp) || StringUtils.isBlank(functionResp.getToolSelection())) {
            return;
        }

        PluginParseResult functionCallParseResult = new PluginParseResult();
        String toolSelection = functionResp.getToolSelection();
        Optional<Plugin> pluginOptional = pluginService.getPluginByName(toolSelection);
        if (pluginOptional.isPresent()) {
            toolSelection = pluginOptional.get().getType();
            functionCallParseResult.setPlugin(pluginOptional.get());
        }
        PluginSemanticQuery semanticQuery = QueryManager.createPluginQuery(toolSelection);

        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        parseInfo.getElementMatches().addAll(queryCtx.getMapInfo().getMatchedElements(domainId));
        functionCallParseResult.setRequest(queryCtx.getRequest());
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, functionCallParseResult);
        parseInfo.setProperties(properties);
        parseInfo.setBonus(FUNCTION_BONUS_THRESHOLD);
        SchemaElement domain = new SchemaElement();
        domain.setDomain(domainId);
        domain.setId(domainId);
        parseInfo.setDomain(domain);
        queryCtx.getCandidateQueries().add(semanticQuery);
    }

    private List<String> getFunctionNames(Long domainId) {
        List<Plugin> plugins = PluginManager.getPlugins();
        Set<String> functionNames = plugins.stream()
                .filter(entry -> ParseMode.FUNCTION_CALL.equals(entry.getParseMode()))
                .filter(entry -> {
                            if (!CollectionUtils.isEmpty(entry.getDomainList())) {
                                return entry.getDomainList().contains(domainId);
                            }
                            return true;
                        }
                ).map(Plugin::getName).collect(Collectors.toSet());
        functionNames.add(DSLQuery.QUERY_MODE);
        return new ArrayList<>(functionNames);
    }

    public FunctionResp requestFunction(String url, FunctionReq functionReq) {
        HttpHeaders headers = new HttpHeaders();
        long startTime = System.currentTimeMillis();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(functionReq), headers);
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(url).build().encode().toUri();
        RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
        try {
            log.info("requestFunction functionReq:{}", functionReq);
            ResponseEntity<FunctionResp> responseEntity = restTemplate.exchange(requestUrl, HttpMethod.POST, entity,
                    FunctionResp.class);
            log.info("requestFunction responseEntity:{},cost:{}", responseEntity,
                    System.currentTimeMillis() - startTime);
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("requestFunction error", e);
        }
        return null;
    }
}
