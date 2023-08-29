package com.tencent.supersonic.chat.parser.plugin.function;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.config.FunctionCallInfoConfig;
import com.tencent.supersonic.chat.parser.ParseMode;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.llm.dsl.DslQuery;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
        List<PluginParseConfig> functionDOList = getFunctionDO(queryCtx.getRequest().getModelId(), queryCtx);
        if (CollectionUtils.isEmpty(functionDOList)) {
            log.info("function call parser, plugin is empty, skip");
            return;
        }
        FunctionResp functionResp = new FunctionResp();
        if (functionDOList.size() == 1) {
            functionResp.setToolSelection(functionDOList.iterator().next().getName());
        } else {
            FunctionReq functionReq = FunctionReq.builder()
                    .queryText(queryCtx.getRequest().getQueryText())
                    .pluginConfigs(functionDOList).build();
            functionResp = requestFunction(functionUrl, functionReq);
        }
        log.info("requestFunction result:{}", functionResp.getToolSelection());
        if (skipFunction(functionResp)) {
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
        plugin.setParseMode(ParseMode.FUNCTION_CALL);
        toolSelection = plugin.getType();
        functionCallParseResult.setPlugin(plugin);
        log.info("QueryManager PluginQueryModes:{}", QueryManager.getPluginQueryModes());
        PluginSemanticQuery semanticQuery = QueryManager.createPluginQuery(toolSelection);
        ModelResolver modelResolver = ComponentFactory.getModelResolver();
        log.info("plugin ModelList:{}", plugin.getModelList());
        Pair<Boolean, Set<Long>> pluginResolveResult = PluginManager.resolve(plugin, queryCtx);
        Long modelId = modelResolver.resolve(queryCtx, chatCtx, pluginResolveResult.getRight());
        log.info("FunctionBasedParser modelId:{}", modelId);
        if ((Objects.isNull(modelId) || modelId <= 0) && !plugin.isContainsAllModel()) {
            log.info("Model is null, skip the parse, select tool: {}", toolSelection);
            return;
        }
        if (!plugin.getModelList().contains(modelId) && !plugin.isContainsAllModel()) {
            return;
        }
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        if (Objects.nonNull(modelId) && modelId > 0) {
            parseInfo.getElementMatches().addAll(queryCtx.getMapInfo().getMatchedElements(modelId));
        }
        functionCallParseResult.setRequest(queryCtx.getRequest());
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, functionCallParseResult);
        properties.put("type", "plugin");
        properties.put("name", plugin.getName());
        parseInfo.setProperties(properties);
        parseInfo.setScore(queryCtx.getRequest().getQueryText().length());
        parseInfo.setQueryMode(semanticQuery.getQueryMode());
        SchemaElement model = new SchemaElement();
        model.setModel(modelId);
        model.setId(modelId);
        parseInfo.setModel(model);
        queryCtx.getCandidateQueries().add(semanticQuery);
    }

    private boolean skipFunction(FunctionResp functionResp) {
        return Objects.isNull(functionResp) || StringUtils.isBlank(functionResp.getToolSelection());
    }

    private List<PluginParseConfig> getFunctionDO(Long modelId, QueryContext queryContext) {
        log.info("user decide Model:{}", modelId);
        List<Plugin> plugins = getPluginList(queryContext);
        List<PluginParseConfig> functionDOList = plugins.stream().filter(plugin -> {
            if (DslQuery.QUERY_MODE.equalsIgnoreCase(plugin.getType())) {
                return false;
            }
            if (plugin.getParseModeConfig() == null) {
                return false;
            }
            PluginParseConfig pluginParseConfig = JsonUtil.toObject(plugin.getParseModeConfig(),
                    PluginParseConfig.class);
            if (StringUtils.isBlank(pluginParseConfig.getName())) {
                return false;
            }
            Pair<Boolean, Set<Long>> pluginResolverResult = PluginManager.resolve(plugin, queryContext);
            log.info("plugin [{}-{}] resolve: {}", plugin.getId(), plugin.getName(), pluginResolverResult);
            if (!pluginResolverResult.getLeft()) {
                return false;
            } else {
                Set<Long> resolveModel = pluginResolverResult.getRight();
                if (modelId != null && modelId > 0) {
                    if (plugin.isContainsAllModel()) {
                        return true;
                    }
                    return resolveModel.contains(modelId);
                }
                return true;
            }
        }).map(o -> JsonUtil.toObject(o.getParseModeConfig(), PluginParseConfig.class)).collect(Collectors.toList());
        log.info("getFunctionDO:{}", JsonUtil.toString(functionDOList));
        return functionDOList;
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

    protected List<Plugin> getPluginList(QueryContext queryContext) {
        return PluginManager.getPluginAgentCanSupport(queryContext.getRequest().getAgentId());
    }
}
