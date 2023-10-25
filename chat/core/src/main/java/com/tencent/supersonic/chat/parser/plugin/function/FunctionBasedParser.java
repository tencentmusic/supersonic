package com.tencent.supersonic.chat.parser.plugin.function;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.parser.ParseMode;
import com.tencent.supersonic.chat.parser.plugin.PluginParser;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.query.llm.s2ql.S2QLQuery;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.common.util.ContextUtils;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
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
public class FunctionBasedParser extends PluginParser {

    @Override
    public boolean checkPreCondition(QueryContext queryContext) {
        FunctionCallConfig functionCallConfig = ContextUtils.getBean(FunctionCallConfig.class);
        String functionUrl = functionCallConfig.getUrl();
        if (StringUtils.isBlank(functionUrl)) {
            log.info("functionUrl:{}, skip function parser, queryText:{}", functionUrl,
                    queryContext.getRequest().getQueryText());
            return false;
        }
        List<Plugin> plugins = getPluginList(queryContext);
        return !CollectionUtils.isEmpty(plugins);
    }

    @Override
    public PluginRecallResult recallPlugin(QueryContext queryContext) {
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        FunctionResp functionResp = functionCall(queryContext);
        if (skipFunction(functionResp)) {
            return null;
        }
        log.info("requestFunction result:{}", functionResp.getToolSelection());
        String toolSelection = functionResp.getToolSelection();
        Optional<Plugin> pluginOptional = pluginService.getPluginByName(toolSelection);
        if (!pluginOptional.isPresent()) {
            log.info("pluginOptional is not exist:{}, skip the parse", toolSelection);
            return null;
        }
        Plugin plugin = pluginOptional.get();
        plugin.setParseMode(ParseMode.FUNCTION_CALL);
        Pair<Boolean, Set<Long>> pluginResolveResult = PluginManager.resolve(plugin, queryContext);
        if (pluginResolveResult.getLeft()) {
            Set<Long> modelList = pluginResolveResult.getRight();
            if (CollectionUtils.isEmpty(modelList)) {
                return null;
            }
            double score = queryContext.getRequest().getQueryText().length();
            return PluginRecallResult.builder().plugin(plugin).modelIds(modelList).score(score).build();
        }
        return null;
    }

    public FunctionResp functionCall(QueryContext queryContext) {
        List<PluginParseConfig> pluginToFunctionCall =
                getPluginToFunctionCall(queryContext.getRequest().getModelId(), queryContext);
        if (CollectionUtils.isEmpty(pluginToFunctionCall)) {
            log.info("function call parser, plugin is empty, skip");
            return null;
        }
        FunctionResp functionResp = new FunctionResp();
        if (pluginToFunctionCall.size() == 1) {
            functionResp.setToolSelection(pluginToFunctionCall.iterator().next().getName());
        } else {
            FunctionReq functionReq = FunctionReq.builder()
                    .queryText(queryContext.getRequest().getQueryText())
                    .pluginConfigs(pluginToFunctionCall).build();
            functionResp = requestFunction(functionReq);
        }
        return functionResp;
    }

    private boolean skipFunction(FunctionResp functionResp) {
        return Objects.isNull(functionResp) || StringUtils.isBlank(functionResp.getToolSelection());
    }

    private List<PluginParseConfig> getPluginToFunctionCall(Long modelId, QueryContext queryContext) {
        log.info("user decide Model:{}", modelId);
        List<Plugin> plugins = getPluginList(queryContext);
        List<PluginParseConfig> functionDOList = plugins.stream().filter(plugin -> {
            if (S2QLQuery.QUERY_MODE.equalsIgnoreCase(plugin.getType())) {
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
        log.info("PluginToFunctionCall: {}", JsonUtil.toString(functionDOList));
        return functionDOList;
    }

    public FunctionResp requestFunction(FunctionReq functionReq) {
        FunctionCallConfig functionCallInfoConfig = ContextUtils.getBean(FunctionCallConfig.class);
        String url = functionCallInfoConfig.getUrl() + functionCallInfoConfig.getPluginSelectPath();
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
