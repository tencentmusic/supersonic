package com.tencent.supersonic.chat.parser.function;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.config.FunctionCallInfoConfig;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginParseConfig;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tencent.supersonic.common.util.JsonUtil;
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

    public static final double SKIP_DSL_LENGTH = 10;


    @Override
    public void parse(QueryContext queryCtx, ChatContext chatCtx) {
        FunctionCallInfoConfig functionCallConfig = ContextUtils.getBean(FunctionCallInfoConfig.class);
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        String functionUrl = functionCallConfig.getUrl();

        if (StringUtils.isBlank(functionUrl) || SatisfactionChecker.check(queryCtx)) {
            log.info("functionUrl:{}, skip function parser, queryText:{}", functionUrl,
                    queryCtx.getRequest().getQueryText());
            return;
        }

        Set<Long> matchedDomains = getMatchDomains(queryCtx);
        List<String> functionNames = getFunctionNames(matchedDomains);
        log.info("matchedDomains:{},functionNames:{}", matchedDomains, functionNames);

        if (CollectionUtils.isEmpty(functionNames) || CollectionUtils.isEmpty(matchedDomains)) {
            return;
        }
        List<PluginParseConfig> functionDOList = getFunctionDO(queryCtx.getRequest().getDomainId());
        FunctionReq functionReq = FunctionReq.builder()
                .queryText(queryCtx.getRequest().getQueryText())
                .pluginConfigs(functionDOList).build();
        FunctionResp functionResp = requestFunction(functionUrl, functionReq);
        log.info("requestFunction result:{}", functionResp.getToolSelection());
        if (skipFunction(queryCtx, functionResp)) {
            return;
        }

        PluginParseResult functionCallParseResult = new PluginParseResult();
        String toolSelection = functionResp.getToolSelection();
        Optional<Plugin> pluginOptional = pluginService.getPluginByName(toolSelection);
        if (!pluginOptional.isPresent()) {
            log.info("pluginOptional is not exist:{}, skip the parse", toolSelection);
            return;
        }
        Plugin plugin = pluginOptional.get();
        toolSelection = plugin.getType();
        functionCallParseResult.setPlugin(plugin);
        log.info("QueryManager PluginQueryModes:{}", QueryManager.getPluginQueryModes());
        PluginSemanticQuery semanticQuery = QueryManager.createPluginQuery(toolSelection);
        DomainResolver domainResolver = ComponentFactory.getDomainResolver();

        Long domainId = domainResolver.resolve(queryCtx, chatCtx, plugin.getDomainList());
        log.info("FunctionBasedParser domainId:{}",domainId);
        if ((Objects.isNull(domainId) || domainId <= 0) && !plugin.isContainsAllDomain()) {
            log.info("domain is null, skip the parse, select tool: {}", toolSelection);
            return;
        }
        if (!plugin.getDomainList().contains(domainId) && !plugin.isContainsAllDomain()) {
            return;
        }
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        if (Objects.nonNull(domainId) && domainId > 0){
            parseInfo.getElementMatches().addAll(queryCtx.getMapInfo().getMatchedElements(domainId));
        }
        functionCallParseResult.setRequest(queryCtx.getRequest());
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, functionCallParseResult);
        parseInfo.setProperties(properties);
        parseInfo.setScore(FUNCTION_BONUS_THRESHOLD);
        parseInfo.setQueryMode(semanticQuery.getQueryMode());
        SchemaElement domain = new SchemaElement();
        domain.setDomain(domainId);
        domain.setId(domainId);
        parseInfo.setDomain(domain);
        queryCtx.getCandidateQueries().add(semanticQuery);
    }

    private Set<Long> getMatchDomains(QueryContext queryCtx) {
        Set<Long> result = new HashSet<>();
        Long domainId = queryCtx.getRequest().getDomainId();
        if (Objects.nonNull(domainId) && domainId > 0) {
            result.add(domainId);
            return result;
        }
        return queryCtx.getMapInfo().getMatchedDomains();
    }

    private boolean skipFunction(QueryContext queryCtx, FunctionResp functionResp) {
        if (Objects.isNull(functionResp) || StringUtils.isBlank(functionResp.getToolSelection())) {
            return true;
        }
        String queryText = queryCtx.getRequest().getQueryText();

        if (functionResp.getToolSelection().equalsIgnoreCase(DSLQuery.QUERY_MODE)
                && queryText.length() < SKIP_DSL_LENGTH) {
            log.info("queryText length is :{}, less than the threshold :{}, skip dsl.", queryText.length(),
                    SKIP_DSL_LENGTH);
            return true;
        }
        return false;
    }

    private List<PluginParseConfig> getFunctionDO(Long domainId) {
        log.info("user decide domain:{}", domainId);
        List<Plugin> plugins = PluginManager.getPlugins();
        List<PluginParseConfig> functionDOList = plugins.stream().filter(o -> {
            if (o.getParseModeConfig() == null) {
                return false;
            }
            if (!CollectionUtils.isEmpty(o.getDomainList())) {//过滤掉没选主题域的插件
                return true;
            }
            if (domainId == null || domainId <= 0L) {
                return true;
            } else {
                return o.getDomainList().contains(domainId);
            }
        }).map(o -> {
            PluginParseConfig functionCallConfig = JsonUtil.toObject(o.getParseModeConfig(),
                    PluginParseConfig.class);
            return functionCallConfig;
        }).collect(Collectors.toList());
        log.info("getFunctionDO:{}", JsonUtil.toString(functionDOList));
        return functionDOList;
    }

    private List<String> getFunctionNames(Set<Long> matchedDomains) {
        List<Plugin> plugins = PluginManager.getPlugins();
        Set<String> functionNames = plugins.stream()
                .filter(entry -> {
                            if (!CollectionUtils.isEmpty(entry.getDomainList()) && !CollectionUtils.isEmpty(matchedDomains)) {
                                return entry.getDomainList().stream().anyMatch(matchedDomains::contains);
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
            log.info("requestFunction functionReq:{}", JsonUtil.toString(functionReq));
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
