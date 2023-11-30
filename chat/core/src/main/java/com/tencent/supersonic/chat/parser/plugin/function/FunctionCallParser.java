package com.tencent.supersonic.chat.parser.plugin.function;

import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.parser.PythonLLMProxy;
import com.tencent.supersonic.chat.parser.LLMProxy;
import com.tencent.supersonic.chat.parser.plugin.ParseMode;
import com.tencent.supersonic.chat.parser.plugin.PluginParser;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

@Slf4j
public class FunctionCallParser extends PluginParser {

    protected LLMProxy llmInterpreter = ComponentFactory.getLLMProxy();

    @Override
    public boolean checkPreCondition(QueryContext queryContext) {
        FunctionCallConfig functionCallConfig = ContextUtils.getBean(FunctionCallConfig.class);
        String functionUrl = functionCallConfig.getUrl();
        if (StringUtils.isBlank(functionUrl) && llmInterpreter instanceof PythonLLMProxy) {
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
            functionResp = llmInterpreter.requestFunction(functionReq);
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
            if (LLMSqlQuery.QUERY_MODE.equalsIgnoreCase(plugin.getType())) {
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

}
