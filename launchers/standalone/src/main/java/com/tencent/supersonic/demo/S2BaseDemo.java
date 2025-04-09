package com.tencent.supersonic.demo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.util.AESEncryptionUtil;
import com.tencent.supersonic.headless.api.pojo.DataSetModelConfig;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.request.DictItemReq;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.DictConfService;
import com.tencent.supersonic.headless.server.service.DictTaskService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.TermService;
import com.tencent.supersonic.headless.server.service.impl.DictWordService;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class S2BaseDemo implements CommandLineRunner {

    protected DatabaseResp demoDatabase;
    protected ChatModel demoChatModel;
    protected User defaultUser = User.getDefaultUser();

    @Autowired
    protected DatabaseService databaseService;
    @Autowired
    protected DomainService domainService;
    @Autowired
    protected ModelService modelService;
    @Autowired
    protected ModelRelaService modelRelaService;
    @Autowired
    protected DimensionService dimensionService;
    @Autowired
    protected MetricService metricService;
    @Autowired
    protected AuthService authService;
    @Autowired
    protected DataSetService dataSetService;
    @Autowired
    protected TermService termService;
    @Autowired
    protected PluginService pluginService;
    @Autowired
    protected DataSourceProperties dataSourceProperties;
    @Autowired
    protected ChatQueryService chatQueryService;
    @Autowired
    protected ChatManageService chatManageService;
    @Autowired
    protected AgentService agentService;
    @Autowired
    protected DictWordService dictWordService;
    @Autowired
    protected ChatModelService chatModelService;
    @Autowired
    protected DictConfService dictConfService;
    @Autowired
    protected DictTaskService dictTaskService;
    @Autowired
    protected Environment environment;

    @Value("${s2.demo.names:S2VisitsDemo}")
    protected List<String> demoList;

    @Value("${spring.datasource.driver-class-name}")
    protected String driverClassName;

    public void run(String... args) {
        demoDatabase = addDatabaseIfNotExist();
        demoChatModel = addChatModelIfNotExist();
        if (demoList != null && demoList.contains(getClass().getSimpleName())) {
            if (checkNeedToRun()) {
                doRun();
            }
        }
    }

    protected abstract void doRun();

    protected abstract boolean checkNeedToRun();

    protected DatabaseResp addDatabaseIfNotExist() {
        List<DatabaseResp> databaseList = databaseService.getDatabaseList(defaultUser);
        if (!CollectionUtils.isEmpty(databaseList)) {
            return databaseList.get(0);
        }
        String url = dataSourceProperties.getUrl();
        DatabaseReq databaseReq = new DatabaseReq();
        databaseReq.setName("S2数据库DEMO");
        databaseReq.setDescription("样例数据库实例仅用于体验");
        databaseReq.setType(DataType.H2.toString());
        if ("org.postgresql.Driver".equals(driverClassName)) {
            databaseReq.setType(DataType.POSTGRESQL.toString());
        } else if ("com.mysql.cj.jdbc.Driver".equals(driverClassName)
                || "com.mysql.jdbc.Driver".equals(driverClassName)) {
            databaseReq.setType(DataType.MYSQL.toString());
            databaseReq.setVersion("5.7");
        }
        databaseReq.setUrl(url);
        databaseReq.setUsername(dataSourceProperties.getUsername());
        databaseReq
                .setPassword(AESEncryptionUtil.aesEncryptECB(dataSourceProperties.getPassword()));
        return databaseService.createOrUpdateDatabase(databaseReq, defaultUser);
    }

    protected ChatModel addChatModelIfNotExist() {
        List<ChatModel> chatModels = chatModelService.getChatModels(defaultUser);
        if (!chatModels.isEmpty()) {
            return chatModels.get(0);
        } else {
            ChatModel chatModel = new ChatModel();
            chatModel.setName("OpenAI模型DEMO");
            chatModel.setDescription("由langchain4j社区提供仅用于体验(单次请求最大token数1000), 正式使用请切换大模型");
            chatModel.setConfig(ModelProvider.DEMO_CHAT_MODEL);
            if (StringUtils.isNotBlank(environment.getProperty("OPENAI_BASE_URL"))) {
                chatModel.getConfig().setBaseUrl(environment.getProperty("OPENAI_BASE_URL"));
            }
            if (StringUtils.isNotBlank(environment.getProperty("OPENAI_API_KEY"))) {
                chatModel.getConfig().setApiKey(environment.getProperty("OPENAI_API_KEY"));
            }
            if (StringUtils.isNotBlank(environment.getProperty("OPENAI_MODEL_NAME"))) {
                chatModel.getConfig().setModelName(environment.getProperty("OPENAI_MODEL_NAME"));
            }
            chatModel = chatModelService.createChatModel(chatModel, defaultUser);
            return chatModel;
        }
    }

    protected MetricResp getMetric(String bizName, ModelResp model) {
        return metricService.getMetric(model.getId(), bizName);
    }

    protected List<DataSetModelConfig> getDataSetModelConfigs(Long domainId) {
        List<DataSetModelConfig> dataSetModelConfigs = Lists.newArrayList();
        List<ModelResp> modelByDomainIds =
                modelService.getModelByDomainIds(Lists.newArrayList(domainId));

        for (ModelResp modelResp : modelByDomainIds) {
            DataSetModelConfig dataSetModelConfig = new DataSetModelConfig();
            dataSetModelConfig.setId(modelResp.getId());
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setModelIds(Lists.newArrayList(modelResp.getId()));
            List<Long> metrics = metricService.getMetrics(metaFilter).stream()
                    .map(MetricResp::getId).collect(Collectors.toList());
            dataSetModelConfig.setMetrics(metrics);
            List<Long> dimensions = dimensionService.getDimensions(metaFilter).stream()
                    .map(DimensionResp::getId).collect(Collectors.toList());
            dataSetModelConfig.setMetrics(metrics);
            dataSetModelConfig.setDimensions(dimensions);
            dataSetModelConfigs.add(dataSetModelConfig);
        }
        return dataSetModelConfigs;
    }

    protected DimensionResp getDimension(String bizName, ModelResp model) {
        return dimensionService.getDimension(bizName, model.getId());
    }

    protected RelateDimension getRelateDimension(List<Long> dimensionIds) {
        RelateDimension relateDimension = new RelateDimension();
        for (Long id : dimensionIds) {
            relateDimension.getDrillDownDimensions().add(new DrillDownDimension(id));
        }
        return relateDimension;
    }

    protected void updateQueryScore(Long queryId) {
        chatManageService.updateFeedback(queryId, 5, "");
    }

    protected void enableDimensionValue(DimensionResp dimension) {
        dictConfService.addDictConf(DictItemReq.builder().type(TypeEnums.DIMENSION)
                .itemId(dimension.getId()).status(StatusEnum.ONLINE).build(), defaultUser);
        dictTaskService.addDictTask(DictSingleTaskReq.builder().itemId(dimension.getId())
                .type(TypeEnums.DIMENSION).build(), defaultUser);
    }

    protected void submitText(int chatId, int agentId, String queryText) {
        chatQueryService.parseAndExecute(ChatParseReq.builder().chatId(chatId).agentId(agentId)
                .queryText(queryText).user(defaultUser).disableLLM(true).build());
    }

    protected void loadDictWord() {
        dictWordService.loadDictWord();
    }
}
