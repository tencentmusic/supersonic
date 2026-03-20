package com.tencent.supersonic.chat.server.listener;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.plugin.ChatPlugin;
import com.tencent.supersonic.chat.api.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.executor.DashboardExecutor;
import com.tencent.supersonic.chat.server.executor.ReportScheduleExecutor;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.service.ExemplarService;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.server.event.TemplateDeployedEvent;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.DimensionConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.ExemplarConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.MeasureConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.ModelConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TemplateDeployedEventListener {

    private final AgentService agentService;
    private final PluginService pluginService;
    private final ChatModelService chatModelService;
    private final ExemplarService exemplarService;
    private final EmbeddingConfig embeddingConfig;
    private final MemoryService memoryService;

    public TemplateDeployedEventListener(AgentService agentService, PluginService pluginService,
            ChatModelService chatModelService, ExemplarService exemplarService,
            EmbeddingConfig embeddingConfig, MemoryService memoryService) {
        this.agentService = agentService;
        this.pluginService = pluginService;
        this.chatModelService = chatModelService;
        this.exemplarService = exemplarService;
        this.embeddingConfig = embeddingConfig;
        this.memoryService = memoryService;
    }

    @EventListener
    public void onTemplateDeployed(TemplateDeployedEvent event) {
        SemanticTemplateConfig config = event.getConfig();
        SemanticDeployResult result = event.getResult();
        User user = event.getUser();

        // Use result.getAgentConfig() which has resolved parameter placeholders
        if (result.getAgentConfig() != null) {
            createAgent(result.getAgentConfig(), result, user);
        }

        // Load template-defined exemplars into agent's memory collection
        if (result.getAgentConfig() != null && result.getAgentConfig().getAgentId() != null
                && !CollectionUtils.isEmpty(config.getExemplars())) {
            loadExemplars(config, result.getAgentConfig().getAgentId(), user);
        }

        if (!CollectionUtils.isEmpty(config.getPlugins())) {
            createPlugins(config.getPlugins(), user);
        }

        // Auto-create REPORT_SCHEDULE plugin and attach to agent (if enabled in template)
        // Skip if plugins config already contains a REPORT_SCHEDULE type plugin
        boolean hasSchedulePluginInConfig =
                !CollectionUtils.isEmpty(config.getPlugins()) && config.getPlugins().stream()
                        .anyMatch(p -> ReportScheduleExecutor.QUERY_MODE.equals(p.getType()));

        if (hasSchedulePluginInConfig) {
            log.info(
                    "Skipping auto-create REPORT_SCHEDULE plugin: already defined in plugins config");
        } else {
            // Check via explicit enableReportSchedulePlugin field (preferred) or chatAppOverrides
            // (legacy)
            boolean enableSchedulePlugin = true; // Default enabled for backward compatibility
            if (config.getAgent() != null) {
                // Prefer explicit field
                if (config.getAgent().getEnableReportSchedulePlugin() != null) {
                    enableSchedulePlugin = config.getAgent().getEnableReportSchedulePlugin();
                } else if (config.getAgent().getChatAppOverrides() != null) {
                    // Fallback to legacy chatAppOverrides
                    Boolean override = config.getAgent().getChatAppOverrides()
                            .get(ReportScheduleExecutor.APP_KEY);
                    if (override != null) {
                        enableSchedulePlugin = override;
                    }
                }
            }

            if (enableSchedulePlugin && result.getAgentConfig() != null
                    && result.getAgentConfig().getDataSetId() != null) {
                Long pluginId = createSchedulePlugin(result.getAgentConfig().getDataSetId(), user);
                if (pluginId != null && result.getAgentConfig().getAgentId() != null) {
                    addPluginToAgent(result.getAgentConfig().getAgentId(), pluginId, user);
                }
            }
        }
    }

    private void createAgent(SemanticDeployResult.AgentConfigResult agentConfig,
            SemanticDeployResult result, User user) {
        try {
            Agent agent = new Agent();
            agent.setName(agentConfig.getName());
            agent.setDescription(agentConfig.getDescription());
            agent.setStatus(1);
            agent.setEnableSearch(Boolean.TRUE.equals(agentConfig.getEnableSearch()) ? 1 : 0);
            agent.setExamples(agentConfig.getExamples() != null ? agentConfig.getExamples()
                    : Collections.emptyList());
            agent.setAdmins(Collections.singletonList(user.getName()));

            // Build toolConfig JSON
            if (agentConfig.getDataSetId() != null) {
                agent.setToolConfig(String.format(
                        "{\"tools\":[{\"id\":\"1\",\"type\":\"DATASET\",\"dataSetIds\":[%d]}]}",
                        agentConfig.getDataSetId()));
            } else {
                agent.setToolConfig("{}");
            }

            // Configure chatAppConfig with all registered ChatApps and chat model
            Map<String, ChatApp> allApps = new HashMap<>(ChatAppManager.getAllApps(AppModule.CHAT));
            setChatModelForApps(allApps, user);
            if (agentConfig.getChatAppOverrides() != null
                    && !agentConfig.getChatAppOverrides().isEmpty()) {
                applyOverrides(allApps, agentConfig.getChatAppOverrides());
            }
            // Enable DASHBOARD_MODE by default for template-deployed agents
            // (only if not explicitly overridden by template config)
            allApps.computeIfPresent(DashboardExecutor.APP_KEY, (k, app) -> {
                if (agentConfig.getChatAppOverrides() == null || !agentConfig.getChatAppOverrides()
                        .containsKey(DashboardExecutor.APP_KEY)) {
                    app.setEnable(true);
                }
                return app;
            });
            agent.setChatAppConfig(allApps);

            Agent created = agentService.createAgent(agent, user);
            agentConfig.setAgentId(created.getId());
            log.info("Auto-created Agent '{}' with ID: {}", agentConfig.getName(), created.getId());
        } catch (Exception e) {
            log.warn(
                    "Failed to auto-create Agent '{}': {}. "
                            + "Please create it manually through the chat module.",
                    agentConfig.getName(), e.getMessage());
        }
    }

    private void setChatModelForApps(Map<String, ChatApp> allApps, User user) {
        Integer chatModelId = 0;
        List<ChatModel> chatModels = chatModelService.getChatModels(user);
        if (!chatModels.isEmpty()) {
            chatModelId = chatModels.getFirst().getId().intValue();
        }
        for (ChatApp app : allApps.values()) {
            app.setChatModelId(chatModelId);
        }
    }

    private void applyOverrides(Map<String, ChatApp> allApps,
            Map<String, Boolean> chatAppOverrides) {
        for (Map.Entry<String, ChatApp> entry : allApps.entrySet()) {
            Boolean override = chatAppOverrides.get(entry.getKey());
            if (override != null) {
                entry.getValue().setEnable(override);
            }
        }
    }

    private void createPlugins(List<PluginConfig> pluginConfigs, User user) {
        for (PluginConfig pc : pluginConfigs) {
            try {
                ChatPlugin plugin = new ChatPlugin();
                plugin.setType(pc.getType());
                plugin.setName(pc.getName());
                plugin.setPattern(pc.getPattern());
                plugin.setDataSetList(pc.getDataSetIds());
                plugin.setConfig(JsonUtil.toString(pc.getConfig()));

                PluginParseConfig parseConfig = PluginParseConfig.builder().name(pc.getName())
                        .description(pc.getDescription()).examples(pc.getExamples()).build();
                plugin.setParseModeConfig(JsonUtil.toString(parseConfig));

                pluginService.createPlugin(plugin, user);
                log.info("Auto-created Plugin '{}'", pc.getName());
            } catch (Exception e) {
                log.warn("Failed to auto-create Plugin '{}': {}", pc.getName(), e.getMessage());
            }
        }
    }

    private Long createSchedulePlugin(Long dataSetId, User user) {
        try {
            ChatPlugin plugin = new ChatPlugin();
            plugin.setType(ReportScheduleExecutor.QUERY_MODE);
            plugin.setName("定时报表");
            plugin.setPattern("定时报表|定时发送|每天发送|定期报告|schedule");
            plugin.setDataSetList(Collections.singletonList(dataSetId));

            PluginParseConfig parseConfig = PluginParseConfig.builder().name("定时报表")
                    .description("通过自然语言创建定时报表任务，支持日报、周报、月报等")
                    .examples(Arrays.asList("每天早上9点发送日报", "每周一发送周报", "每月1号发送月度运营报告")).build();
            plugin.setParseModeConfig(JsonUtil.toString(parseConfig));

            pluginService.createPlugin(plugin, user);

            // Retrieve the newly created plugin (last in list) to get its ID
            List<ChatPlugin> allPlugins = pluginService.getPluginList();
            ChatPlugin created = allPlugins.get(allPlugins.size() - 1);
            log.info("Auto-created REPORT_SCHEDULE plugin with ID: {}", created.getId());
            return created.getId();
        } catch (Exception e) {
            log.warn("Failed to auto-create REPORT_SCHEDULE plugin: {}", e.getMessage());
            return null;
        }
    }

    private void addPluginToAgent(Integer agentId, Long pluginId, User user) {
        try {
            Agent agent = agentService.getAgent(agentId);
            String toolConfig = agent.getToolConfig();
            JSONObject toolJson = JSONObject.parseObject(toolConfig);
            if (toolJson == null) {
                toolJson = new JSONObject();
            }
            JSONArray tools = toolJson.getJSONArray("tools");
            if (tools == null) {
                tools = new JSONArray();
            }

            JSONObject pluginTool = new JSONObject();
            pluginTool.put("id", String.valueOf(tools.size() + 1));
            pluginTool.put("type", "PLUGIN");
            pluginTool.put("plugins", Collections.singletonList(pluginId));
            tools.add(pluginTool);
            toolJson.put("tools", tools);

            agent.setToolConfig(toolJson.toJSONString());
            agentService.updateAgent(agent, user);
            log.info("Added REPORT_SCHEDULE plugin {} to Agent {}", pluginId, agentId);
        } catch (Exception e) {
            log.warn("Failed to add REPORT_SCHEDULE plugin to Agent {}: {}", agentId,
                    e.getMessage());
        }
    }

    private void loadExemplars(SemanticTemplateConfig config, Integer agentId, User user) {
        try {
            String dbSchema = buildDbSchema(config);
            String collectionName = embeddingConfig.getMemoryCollectionName(agentId);
            int count = 0;

            for (ExemplarConfig ec : config.getExemplars()) {
                if (StringUtils.isBlank(ec.getQuestion()) || StringUtils.isBlank(ec.getSql())) {
                    continue;
                }

                // Store to vector DB for similarity retrieval
                Text2SQLExemplar exemplar = Text2SQLExemplar.builder().question(ec.getQuestion())
                        .dbSchema(dbSchema).sql(ec.getSql()).build();
                exemplarService.storeExemplar(collectionName, exemplar);

                // Persist as ChatMemory record so it survives app restarts
                ChatMemory memory = ChatMemory.builder().agentId(agentId).question(ec.getQuestion())
                        .dbSchema(dbSchema).s2sql(ec.getSql()).status(MemoryStatus.ENABLED)
                        .createdBy("template").createdAt(new Date()).build();
                memoryService.createMemory(memory);

                count++;
            }
            log.info("Loaded {} template exemplars for Agent {}", count, agentId);
        } catch (Exception e) {
            log.warn("Failed to load template exemplars for Agent {}: {}", agentId, e.getMessage());
        }
    }

    private String buildDbSchema(SemanticTemplateConfig config) {
        String tableName = config.getDataSet() != null ? config.getDataSet().getName() : "";
        String partitionTimeStr = "";
        List<String> metrics = new ArrayList<>();
        List<String> dimensions = new ArrayList<>();

        if (!CollectionUtils.isEmpty(config.getModels())) {
            for (ModelConfig model : config.getModels()) {
                // Collect metrics from measures
                if (!CollectionUtils.isEmpty(model.getMeasures())) {
                    for (MeasureConfig measure : model.getMeasures()) {
                        StringBuilder metricStr = new StringBuilder();
                        metricStr.append("<").append(measure.getName());
                        if (StringUtils.isNotEmpty(measure.getAggOperator())) {
                            metricStr.append(" AGGREGATE '")
                                    .append(measure.getAggOperator().toUpperCase()).append("'");
                        }
                        metricStr.append(">");
                        metrics.add(metricStr.toString());
                    }
                }

                // Collect dimensions
                if (!CollectionUtils.isEmpty(model.getDimensions())) {
                    for (DimensionConfig dim : model.getDimensions()) {
                        if ("partition_time".equals(dim.getType())) {
                            String format = StringUtils.isNotEmpty(dim.getDateFormat())
                                    ? dim.getDateFormat()
                                    : "yyyy-MM-dd";
                            partitionTimeStr =
                                    String.format("%s FORMAT '%s'", dim.getName(), format);
                        }
                        StringBuilder dimStr = new StringBuilder();
                        dimStr.append("<").append(dim.getName());
                        if (StringUtils.isNotEmpty(dim.getAlias())) {
                            dimStr.append(" ALIAS '").append(dim.getAlias()).append("'");
                        }
                        if (StringUtils.isNotEmpty(dim.getDateFormat())) {
                            dimStr.append(" FORMAT '").append(dim.getDateFormat()).append("'");
                        }
                        dimStr.append(">");
                        dimensions.add(dimStr.toString());
                    }
                }
            }
        }

        return String.format(
                "DatabaseType=[], DatabaseVersion=[], Table=[%s], PartitionTimeField=[%s], "
                        + "PrimaryKeyField=[], Metrics=[%s], Dimensions=[%s], Values=[]",
                tableName, partitionTimeStr, String.join(",", metrics),
                String.join(",", dimensions));
    }
}
