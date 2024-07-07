package com.tencent.supersonic.demo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.service.SystemConfigService;
import com.tencent.supersonic.headless.api.pojo.DataSetModelConfig;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.web.service.CanvasService;
import com.tencent.supersonic.headless.server.web.service.DataSetService;
import com.tencent.supersonic.headless.server.web.service.DatabaseService;
import com.tencent.supersonic.headless.server.web.service.DimensionService;
import com.tencent.supersonic.headless.server.web.service.DomainService;
import com.tencent.supersonic.headless.server.web.service.MetricService;
import com.tencent.supersonic.headless.server.web.service.ModelRelaService;
import com.tencent.supersonic.headless.server.web.service.ModelService;
import com.tencent.supersonic.headless.server.web.service.TagMetaService;
import com.tencent.supersonic.headless.server.web.service.TagObjectService;
import com.tencent.supersonic.headless.server.web.service.TermService;
import com.tencent.supersonic.headless.server.web.service.impl.DictWordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class S2BaseDemo implements CommandLineRunner {
    protected DatabaseResp demoDatabaseResp;

    protected User user = User.getFakeUser();
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
    protected TagMetaService tagMetaService;
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
    protected TagObjectService tagObjectService;
    @Autowired
    protected ChatService chatService;
    @Autowired
    protected ChatManageService chatManageService;
    @Autowired
    protected AgentService agentService;
    @Autowired
    protected SystemConfigService sysParameterService;
    @Autowired
    protected CanvasService canvasService;
    @Autowired
    protected DictWordService dictWordService;
    @Value("${s2.demo.names:S2VisitsDemo}")
    protected List<String> demoList;
    @Value("${s2.demo.enableLLM:true}")
    protected boolean demoEnableLlm;

    public void run(String... args) {
        demoDatabaseResp = addDatabaseIfNotExist();
        if (demoList != null && demoList.contains(getClass().getSimpleName())) {
            if (checkNeedToRun()) {
                doRun();
            }
        }
    }

    abstract void doRun();

    abstract boolean checkNeedToRun();

    protected DatabaseResp addDatabaseIfNotExist() {
        List<DatabaseResp> databaseList = databaseService.getDatabaseList(User.getFakeUser());
        if (!CollectionUtils.isEmpty(databaseList)) {
            return databaseList.get(0);
        }
        String url = dataSourceProperties.getUrl();
        DatabaseReq databaseReq = new DatabaseReq();
        databaseReq.setName("数据实例");
        databaseReq.setDescription("样例数据库实例");
        if (StringUtils.isNotBlank(url)
                && url.toLowerCase().contains(DataType.MYSQL.getFeature().toLowerCase())) {
            databaseReq.setType(DataType.MYSQL.getFeature());
            databaseReq.setVersion("5.7");
        } else {
            databaseReq.setType(DataType.H2.getFeature());
        }
        databaseReq.setUrl(url);
        databaseReq.setUsername(dataSourceProperties.getUsername());
        databaseReq.setPassword(dataSourceProperties.getPassword());
        return databaseService.createOrUpdateDatabase(databaseReq, user);
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
            List<Long> metrics = metricService.getMetrics(metaFilter)
                    .stream().map(MetricResp::getId).collect(Collectors.toList());
            dataSetModelConfig.setMetrics(metrics);
            List<Long> dimensions = dimensionService.getDimensions(metaFilter)
                    .stream().map(DimensionResp::getId).collect(Collectors.toList());
            dataSetModelConfig.setMetrics(metrics);
            dataSetModelConfig.setDimensions(dimensions);
            dataSetModelConfigs.add(dataSetModelConfig);
        }
        return dataSetModelConfigs;
    }

    protected void addTag(Long itemId, TagDefineType tagDefineType) {
        TagReq tagReq = new TagReq();
        tagReq.setTagDefineType(tagDefineType);
        tagReq.setItemId(itemId);
        tagMetaService.create(tagReq, User.getFakeUser());
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

    protected void updateQueryScore(Integer queryId) {
        chatManageService.updateFeedback(queryId, 5, "");
    }

}
