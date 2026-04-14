package com.tencent.supersonic.headless.server.executor;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.config.TemplateConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig.*;
import com.tencent.supersonic.headless.api.pojo.request.DictItemReq;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.pojo.*;
import com.tencent.supersonic.headless.server.service.*;
import com.tencent.supersonic.headless.server.service.impl.DictWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Initializes builtin semantic templates at application startup and optionally auto-deploys them.
 * Also creates supplementary demo data (auth groups, plugins, dict config) for specific templates.
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class BuiltinSemanticTemplateInitializer implements CommandLineRunner {

    private final SemanticTemplateService semanticTemplateService;
    private final UserService userService;
    private final TemplateConfig templateConfig;
    private final DatabaseService databaseService;
    private final AuthService authService;
    private final DimensionService dimensionService;
    private final ModelService modelService;
    private final DictConfService dictConfService;
    private final DictTaskService dictTaskService;
    private final DictWordService dictWordService;

    @Override
    public void run(String... args) {
        try {
            initBuiltinTemplates();
        } catch (Exception e) {
            log.error("Failed to initialize builtin templates", e);
        }

        if (templateConfig.isAutoDeploy()) {
            try {
                autoDeployBuiltinTemplates();
            } catch (Exception e) {
                log.error("Failed to auto-deploy builtin templates", e);
            }
        }
    }

    /**
     * Initializes builtin semantic templates if they do not already exist.
     */
    private void initBuiltinTemplates() {
        List<SemanticTemplate> existingTemplates = semanticTemplateService.getBuiltinTemplates();
        if (!existingTemplates.isEmpty()) {
            log.info("Builtin templates already exist, skipping initialization");
            return;
        }

        log.info("Initializing builtin semantic templates...");
        User adminUser = userService.getDefaultUser();
        initVisitsTemplate(adminUser);
        initSingerTemplate(adminUser);
        initCompanyTemplate(adminUser);
        initSmallTalkTemplate(adminUser);
        log.info("Builtin semantic templates initialized successfully");
    }

    /**
     * Auto-deploys builtin templates that are not yet deployed.
     */
    private void autoDeployBuiltinTemplates() {
        User user = userService.getDefaultUser();

        // Get database for templates that need semantic models
        List<DatabaseResp> databases = databaseService.getDatabaseList(user);
        Long databaseId = CollectionUtils.isEmpty(databases) ? null : databases.getFirst().getId();

        List<SemanticTemplate> builtinTemplates = semanticTemplateService.getBuiltinTemplates();
        if (builtinTemplates.isEmpty()) {
            return;
        }

        // Find already deployed template IDs
        List<SemanticDeployment> deployments = semanticTemplateService.getDeploymentHistory(user);
        Set<Long> deployedTemplateIds = deployments.stream()
                .filter(d -> d.getStatus() == SemanticDeployment.DeploymentStatus.SUCCESS)
                .map(SemanticDeployment::getTemplateId).collect(Collectors.toSet());

        for (SemanticTemplate template : builtinTemplates) {
            if (deployedTemplateIds.contains(template.getId())) {
                log.info("Builtin template '{}' already deployed, skipping", template.getName());
                continue;
            }

            // Check if this template requires a database (has domain config)
            boolean needsDatabase = template.getTemplateConfig() != null
                    && template.getTemplateConfig().getDomain() != null;
            if (needsDatabase && databaseId == null) {
                log.warn("No database available, skipping template: {}", template.getName());
                continue;
            }

            try {
                SemanticDeployParam param =
                        buildDefaultDeployParam(template, needsDatabase ? databaseId : null);
                log.info("Auto-deploying builtin template: {}", template.getName());
                SemanticDeployment deployment =
                        semanticTemplateService.executeDeployment(template.getId(), param, user);

                if (deployment.getStatus() == SemanticDeployment.DeploymentStatus.SUCCESS) {
                    log.info("Successfully auto-deployed template: {}", template.getName());
                    postDeployVisitsExtras(template, deployment, user);
                } else {
                    log.error("Failed to auto-deploy template: {}, error: {}", template.getName(),
                            deployment.getErrorMessage());
                }
            } catch (Exception e) {
                log.error("Failed to auto-deploy template: {}", template.getName(), e);
            }
        }

        // Load dict words after all templates are deployed
        try {
            dictWordService.loadDictWord();
        } catch (Exception e) {
            log.error("Failed to load dict words after auto-deploy", e);
        }
    }

    private SemanticDeployParam buildDefaultDeployParam(SemanticTemplate template,
            Long databaseId) {
        SemanticDeployParam param = new SemanticDeployParam();
        param.setDatabaseId(databaseId);
        Map<String, String> params = new HashMap<>();
        SemanticTemplateConfig config = template.getTemplateConfig();
        if (config != null && !CollectionUtils.isEmpty(config.getConfigParams())) {
            for (ConfigParam cp : config.getConfigParams()) {
                if (cp.getDefaultValue() != null) {
                    params.put(cp.getKey(), cp.getDefaultValue());
                }
            }
        }
        param.setParams(params);
        return param;
    }

    /**
     * Post-deploy extras for Visits template: auth groups, plugin, dict config.
     * 
     * @param template The deployed semantic template
     * @param deployment Thedeployment record
     * @param user The userperforming the deployment
     */
    private void postDeployVisitsExtras(SemanticTemplate template, SemanticDeployment deployment,
            User user) {
        if (!"visits_template".equals(template.getBizName())) {
            return;
        }
        SemanticDeployResult result = deployment.getResultDetail();
        if (result == null || result.getDomainId() == null) {
            return;
        }
        try {
            // Find the stay time model for auth groups
            Long domainId = result.getDomainId();
            List<ModelResp> models =
                    modelService.getModelByDomainIds(Collections.singletonList(domainId));
            models.stream().filter(m -> "s2_stay_time_statis".equals(m.getBizName())).findFirst()
                    .ifPresent(this::addVisitsAuthGroups);

            // Enable dict for department and user_name dimensions
            enableDictForVisits(domainId, user);
        } catch (Exception e) {
            log.warn("Failed to create Visits extras (auth/dict/plugin): {}", e.getMessage());
        }
    }

    private void addVisitsAuthGroups(ModelResp stayTimeModel) {
        // Column permission for jack
        AuthGroup columnPerm = new AuthGroup();
        columnPerm.setModelId(stayTimeModel.getId());
        columnPerm.setName("jack_column_permission");
        AuthRule authRule = new AuthRule();
        authRule.setMetrics(Collections.singletonList("stay_hours"));
        authRule.setDimensions(Collections.singletonList("page"));
        columnPerm.setAuthRules(Collections.singletonList(authRule));
        columnPerm.setAuthorizedUsers(Collections.singletonList("jack"));
        columnPerm.setAuthorizedDepartmentIds(Collections.emptyList());
        authService.addOrUpdateAuthGroup(columnPerm);

        // Row permission for tom
        AuthGroup rowPerm = new AuthGroup();
        rowPerm.setModelId(stayTimeModel.getId());
        rowPerm.setName("tom_row_permission");
        rowPerm.setAuthRules(Collections.emptyList());
        rowPerm.setDimensionFilters(Collections.singletonList("user_name = 'tom'"));
        rowPerm.setAuthorizedUsers(Collections.singletonList("tom"));
        rowPerm.setAuthorizedDepartmentIds(Collections.emptyList());
        authService.addOrUpdateAuthGroup(rowPerm);
        log.info("Created Visits auth groups");
    }

    private void enableDictForVisits(Long domainId, User user) {
        List<ModelResp> models =
                modelService.getModelByDomainIds(Collections.singletonList(domainId));
        for (ModelResp model : models) {
            MetaFilter filter = new MetaFilter();
            filter.setModelIds(Collections.singletonList(model.getId()));
            List<DimensionResp> dims = dimensionService.getDimensions(filter);
            for (DimensionResp dim : dims) {
                if ("department".equals(dim.getBizName()) || "user_name".equals(dim.getBizName())) {
                    dictConfService.addDictConf(DictItemReq.builder().type(TypeEnums.DIMENSION)
                            .itemId(dim.getId()).status(StatusEnum.ONLINE).build(), user);
                    dictTaskService.addDictTask(DictSingleTaskReq.builder().itemId(dim.getId())
                            .type(TypeEnums.DIMENSION).build(), user);
                }
            }
        }
        log.info("Enabled dict for Visits dimensions");
    }

    /**
     * Initialize Visits Semantic Template
     * 
     * @param user The user performing the initialization
     */
    private void initVisitsTemplate(User user) {
        SemanticTemplate template = new SemanticTemplate();
        template.setName("访问统计模板");
        template.setBizName("visits_template");
        template.setCategory("VISITS");
        template.setDescription("用于分析产品访问情况，包含用户、PV/UV、停留时长等指标。适合产品运营分析场景。");
        template.setStatus(1);

        SemanticTemplateConfig config = new SemanticTemplateConfig();

        // Config params
        List<ConfigParam> params = new ArrayList<>();
        params.add(createParam("domain_name", "域名称", "TEXT", "超音数", true, "语义域名称"));
        params.add(createParam("domain_bizname", "域代码", "TEXT", "supersonic", true, "语义域代码"));
        params.add(
                createParam("table_user", "用户表", "TABLE", "s2_user_department", true, "用户部门信息表"));
        params.add(createParam("table_pv_uv", "PV/UV表", "TABLE", "s2_pv_uv_statis", true, "访问统计表"));
        params.add(createParam("table_stay", "停留时长表", "TABLE", "s2_stay_time_statis", true,
                "停留时长统计表"));
        config.setConfigParams(params);

        // Domain
        DomainConfig domain = new DomainConfig();
        domain.setName("${domain_name}");
        domain.setBizName("${domain_bizname}");
        domain.setDescription("产品访问数据域");
        domain.setIsOpen(1);
        config.setDomain(domain);

        // Model 1: User Department
        ModelConfig userModel = new ModelConfig();
        userModel.setName("用户部门");
        userModel.setBizName("user_department");
        userModel.setDescription("用户部门信息");
        userModel.setTableName("${table_user}");

        IdentifyConfig userIdentify = new IdentifyConfig();
        userIdentify.setName("用户名");
        userIdentify.setBizName("user_name");
        userIdentify.setFieldName("user_name");
        userIdentify.setType("primary");
        userModel.setIdentifiers(Collections.singletonList(userIdentify));

        DimensionConfig deptDim = new DimensionConfig();
        deptDim.setName("部门");
        deptDim.setBizName("department");
        deptDim.setFieldName("department");
        deptDim.setType("categorical");
        deptDim.setEnableDictValue(true);
        userModel.setDimensions(Collections.singletonList(deptDim));

        // Model 2: PV/UV Stats
        ModelConfig pvUvModel = new ModelConfig();
        pvUvModel.setName("PVUV统计");
        pvUvModel.setBizName("s2_pv_uv_statis");
        pvUvModel.setDescription("访问次数和用户数统计");
        pvUvModel.setTableName("${table_pv_uv}");

        IdentifyConfig pvIdentify = new IdentifyConfig();
        pvIdentify.setName("用户名");
        pvIdentify.setBizName("user_name");
        pvIdentify.setFieldName("user_name");
        pvIdentify.setType("foreign");
        pvUvModel.setIdentifiers(Collections.singletonList(pvIdentify));

        List<DimensionConfig> pvDims = new ArrayList<>();
        DimensionConfig dateDim = new DimensionConfig();
        dateDim.setName("数据日期");
        dateDim.setBizName("imp_date");
        dateDim.setFieldName("imp_date");
        dateDim.setType("partition_time");
        pvDims.add(dateDim);

        DimensionConfig pageDim = new DimensionConfig();
        pageDim.setName("页面");
        pageDim.setBizName("page");
        pageDim.setFieldName("page");
        pageDim.setType("categorical");
        pvDims.add(pageDim);
        pvUvModel.setDimensions(pvDims);

        // Model 3: Stay Time Stats
        ModelConfig stayModel = new ModelConfig();
        stayModel.setName("停留时长统计");
        stayModel.setBizName("s2_stay_time_statis");
        stayModel.setDescription("用户停留时长统计");
        stayModel.setTableName("${table_stay}");

        IdentifyConfig stayIdentify = new IdentifyConfig();
        stayIdentify.setName("用户名");
        stayIdentify.setBizName("user_name");
        stayIdentify.setFieldName("user_name");
        stayIdentify.setType("foreign");
        stayModel.setIdentifiers(Collections.singletonList(stayIdentify));

        List<DimensionConfig> stayDims = new ArrayList<>();
        DimensionConfig stayDateDim = new DimensionConfig();
        stayDateDim.setName("数据日期");
        stayDateDim.setBizName("imp_date");
        stayDateDim.setFieldName("imp_date");
        stayDateDim.setType("partition_time");
        stayDims.add(stayDateDim);

        DimensionConfig stayPageDim = new DimensionConfig();
        stayPageDim.setName("页面");
        stayPageDim.setBizName("visits_page");
        stayPageDim.setFieldName("page");
        stayPageDim.setType("categorical");
        stayPageDim.setExpr("page");
        stayDims.add(stayPageDim);
        stayModel.setDimensions(stayDims);

        MeasureConfig stayMeasure = new MeasureConfig();
        stayMeasure.setName("停留时长");
        stayMeasure.setBizName("stay_hours");
        stayMeasure.setFieldName("stay_hours");
        stayMeasure.setAggOperator(AggregateTypeEnum.SUM.name());
        stayMeasure.setCreateMetric(true);
        stayModel.setMeasures(Collections.singletonList(stayMeasure));

        config.setModels(Arrays.asList(userModel, pvUvModel, stayModel));

        // Model Relations
        List<ModelRelationConfig> relations = new ArrayList<>();
        relations.add(createRelation("s2_pv_uv_statis", "user_department", "user_name"));
        relations.add(createRelation("s2_stay_time_statis", "user_department", "user_name"));
        config.setModelRelations(relations);

        // DataSet
        DataSetConfig dataSet = new DataSetConfig();
        dataSet.setName("${domain_name}数据集");
        dataSet.setBizName("${domain_bizname}_dataset");
        dataSet.setDescription("包含访问统计相关的指标和维度");
        config.setDataSet(dataSet);

        // Agent
        AgentConfig agent = new AgentConfig();
        agent.setName("${domain_name}分析助手");
        agent.setDescription("帮助您分析产品的用户访问情况");
        agent.setEnableSearch(true);
        agent.setExamples(Arrays.asList("近15天访问次数汇总", "按部门统计访问人数", "过去30天访问次数最高的部门top3",
                "近1个月总访问次数超过100次的部门有几个"));
        config.setAgent(agent);

        // Terms
        List<TermConfig> terms = new ArrayList<>();
        TermConfig recentTerm = new TermConfig();
        recentTerm.setName("近期");
        recentTerm.setDescription("指近10天");
        recentTerm.setAlias(Collections.singletonList("近一段时间"));
        terms.add(recentTerm);

        TermConfig vipTerm = new TermConfig();
        vipTerm.setName("核心用户");
        vipTerm.setDescription("用户为tom和lucy");
        vipTerm.setAlias(Collections.singletonList("VIP用户"));
        terms.add(vipTerm);
        config.setTerms(terms);

        // Plugin config
        PluginConfig plugin = new PluginConfig();
        plugin.setType("WebServiceQuery");
        plugin.setName("超音数流量分析小助手");
        plugin.setDescription("用于分析超音数的流量概况，包含UV、PV等核心指标的追踪。P.S. 仅作为示例展示，无实际内容");
        plugin.setPattern("用于分析超音数的流量概况，包含UV、PV等核心指标的追踪。P.S. 仅作为示例展示，无实际内容");
        plugin.setExamples(Collections.singletonList("tom最近访问超音数情况怎么样"));
        plugin.setDataSetIds(Collections.singletonList(-1L));
        plugin.setConfig(Map.of("url", "http://localhost:9080/api/chat/plugin/pluginDemo",
                "paramOptions", List.of()));
        config.setPlugins(Collections.singletonList(plugin));

        template.setTemplateConfig(config);
        semanticTemplateService.saveBuiltinTemplate(template, user);
        log.info("Initialized visits template");
    }

    /*
     * Initialize Singer Semantic Template
     * 
     * @param user The user performing the initialization
     */
    private void initSingerTemplate(User user) {
        SemanticTemplate template = new SemanticTemplate();
        template.setName("歌手库模板");
        template.setBizName("singer_template");
        template.setCategory("SINGER");
        template.setDescription("用于分析歌手和歌曲数据。适合音乐、娱乐行业数据分析场景。");
        template.setStatus(1);

        SemanticTemplateConfig config = new SemanticTemplateConfig();

        List<ConfigParam> params = new ArrayList<>();
        params.add(createParam("domain_name", "域名称", "TEXT", "艺人", true, "语义域名称"));
        params.add(createParam("domain_bizname", "域代码", "TEXT", "singer", true, "语义域代码"));
        params.add(createParam("table_singer", "歌手表", "TABLE", "singer", true, "歌手信息表"));
        config.setConfigParams(params);

        DomainConfig domain = new DomainConfig();
        domain.setName("${domain_name}");
        domain.setBizName("${domain_bizname}");
        domain.setDescription("歌手音乐数据域");
        domain.setIsOpen(1);
        config.setDomain(domain);

        ModelConfig singerModel = new ModelConfig();
        singerModel.setName("歌手库");
        singerModel.setBizName("singer");
        singerModel.setDescription("歌手基本信息和歌曲数据");
        singerModel.setTableName("${table_singer}");

        IdentifyConfig singerIdentify = new IdentifyConfig();
        singerIdentify.setName("歌手名");
        singerIdentify.setBizName("singer_name");
        singerIdentify.setFieldName("singer_name");
        singerIdentify.setType("primary");
        singerModel.setIdentifiers(Collections.singletonList(singerIdentify));

        List<DimensionConfig> singerDims = new ArrayList<>();
        DimensionConfig actDateDim = new DimensionConfig();
        actDateDim.setName("活跃区间");
        actDateDim.setBizName("act_area");
        actDateDim.setFieldName("act_area");
        actDateDim.setType("categorical");
        singerDims.add(actDateDim);

        DimensionConfig genreDim = new DimensionConfig();
        genreDim.setName("流派");
        genreDim.setBizName("genre");
        genreDim.setFieldName("genre");
        genreDim.setType("categorical");
        genreDim.setEnableDictValue(true);
        singerDims.add(genreDim);
        singerModel.setDimensions(singerDims);

        List<MeasureConfig> singerMeasures = new ArrayList<>();
        MeasureConfig songMeasure = new MeasureConfig();
        songMeasure.setName("歌曲数");
        songMeasure.setBizName("song_count");
        songMeasure.setFieldName("song_count");
        songMeasure.setAggOperator(AggregateTypeEnum.SUM.name());
        songMeasure.setCreateMetric(true);
        singerMeasures.add(songMeasure);

        MeasureConfig playMeasure = new MeasureConfig();
        playMeasure.setName("播放量");
        playMeasure.setBizName("play_count");
        playMeasure.setFieldName("play_count");
        playMeasure.setAggOperator(AggregateTypeEnum.SUM.name());
        playMeasure.setCreateMetric(true);
        singerMeasures.add(playMeasure);
        singerModel.setMeasures(singerMeasures);

        config.setModels(Collections.singletonList(singerModel));

        DataSetConfig dataSet = new DataSetConfig();
        dataSet.setName("${domain_name}数据集");
        dataSet.setBizName("${domain_bizname}_dataset");
        dataSet.setDescription("包含歌手相关的指标和维度");
        config.setDataSet(dataSet);

        AgentConfig agent = new AgentConfig();
        agent.setName("${domain_name}分析助手");
        agent.setDescription("帮助您分析歌手和歌曲数据");
        agent.setEnableSearch(true);
        agent.setExamples(Arrays.asList("歌曲数最多的歌手", "按流派统计歌手数量", "播放量最高的歌手top10", "周杰伦的歌曲数和播放量"));
        config.setAgent(agent);

        template.setTemplateConfig(config);
        semanticTemplateService.saveBuiltinTemplate(template, user);
        log.info("Initialized singer template");
    }

    /**
     * Initialize Company Semantic Template
     * 
     * @param user The user performing the initialization
     */
    private void initCompanyTemplate(User user) {
        SemanticTemplate template = new SemanticTemplate();
        template.setName("企业分析模板");
        template.setBizName("company_template");
        template.setCategory("COMPANY");
        template.setDescription("用于分析企业、品牌和收入数据。适合企业经营分析场景。");
        template.setStatus(1);

        SemanticTemplateConfig config = new SemanticTemplateConfig();

        List<ConfigParam> params = new ArrayList<>();
        params.add(createParam("domain_name", "域名称", "TEXT", "企业", true, "语义域名称"));
        params.add(createParam("domain_bizname", "域代码", "TEXT", "company", true, "语义域代码"));
        params.add(createParam("table_company", "企业表", "TABLE", "company", true, "企业信息表"));
        params.add(createParam("table_brand", "品牌表", "TABLE", "brand", true, "品牌信息表"));
        params.add(createParam("table_revenue", "收入表", "TABLE", "brand_revenue", true, "品牌收入表"));
        config.setConfigParams(params);

        DomainConfig domain = new DomainConfig();
        domain.setName("${domain_name}");
        domain.setBizName("${domain_bizname}");
        domain.setDescription("企业经营数据域");
        domain.setIsOpen(1);
        config.setDomain(domain);

        ModelConfig companyModel = new ModelConfig();
        companyModel.setName("企业信息");
        companyModel.setBizName("company");
        companyModel.setDescription("企业基本信息");
        companyModel.setTableName("${table_company}");

        IdentifyConfig companyIdentify = new IdentifyConfig();
        companyIdentify.setName("公司名");
        companyIdentify.setBizName("company_name");
        companyIdentify.setFieldName("company_name");
        companyIdentify.setType("primary");
        companyModel.setIdentifiers(Collections.singletonList(companyIdentify));

        DimensionConfig industryDim = new DimensionConfig();
        industryDim.setName("行业");
        industryDim.setBizName("industry");
        industryDim.setFieldName("industry");
        industryDim.setType("categorical");
        industryDim.setEnableDictValue(true);
        companyModel.setDimensions(Collections.singletonList(industryDim));

        ModelConfig brandModel = new ModelConfig();
        brandModel.setName("品牌信息");
        brandModel.setBizName("brand");
        brandModel.setDescription("品牌信息");
        brandModel.setTableName("${table_brand}");

        IdentifyConfig brandIdentify = new IdentifyConfig();
        brandIdentify.setName("品牌名");
        brandIdentify.setBizName("brand_name");
        brandIdentify.setFieldName("brand_name");
        brandIdentify.setType("primary");

        IdentifyConfig brandCompanyIdentify = new IdentifyConfig();
        brandCompanyIdentify.setName("公司名");
        brandCompanyIdentify.setBizName("company_name");
        brandCompanyIdentify.setFieldName("company_name");
        brandCompanyIdentify.setType("foreign");
        brandModel.setIdentifiers(Arrays.asList(brandIdentify, brandCompanyIdentify));

        DimensionConfig categoryDim = new DimensionConfig();
        categoryDim.setName("品类");
        categoryDim.setBizName("category");
        categoryDim.setFieldName("category");
        categoryDim.setType("categorical");
        brandModel.setDimensions(Collections.singletonList(categoryDim));

        ModelConfig revenueModel = new ModelConfig();
        revenueModel.setName("品牌收入");
        revenueModel.setBizName("brand_revenue");
        revenueModel.setDescription("品牌收入数据");
        revenueModel.setTableName("${table_revenue}");

        IdentifyConfig revBrandIdentify = new IdentifyConfig();
        revBrandIdentify.setName("品牌名");
        revBrandIdentify.setBizName("brand_name");
        revBrandIdentify.setFieldName("brand_name");
        revBrandIdentify.setType("foreign");
        revenueModel.setIdentifiers(Collections.singletonList(revBrandIdentify));

        DimensionConfig yearDim = new DimensionConfig();
        yearDim.setName("年份");
        yearDim.setBizName("year");
        yearDim.setFieldName("year");
        yearDim.setType("partition_time");
        revenueModel.setDimensions(Collections.singletonList(yearDim));

        MeasureConfig revenueMeasure = new MeasureConfig();
        revenueMeasure.setName("收入金额");
        revenueMeasure.setBizName("revenue");
        revenueMeasure.setFieldName("revenue");
        revenueMeasure.setAggOperator(AggregateTypeEnum.SUM.name());
        revenueMeasure.setCreateMetric(true);
        revenueModel.setMeasures(Collections.singletonList(revenueMeasure));

        config.setModels(Arrays.asList(companyModel, brandModel, revenueModel));

        List<ModelRelationConfig> relations = new ArrayList<>();
        relations.add(createRelation("brand", "company", "company_name"));
        relations.add(createRelation("brand_revenue", "brand", "brand_name"));
        config.setModelRelations(relations);

        DataSetConfig dataSet = new DataSetConfig();
        dataSet.setName("${domain_name}数据集");
        dataSet.setBizName("${domain_bizname}_dataset");
        dataSet.setDescription("包含企业经营相关的指标和维度");
        config.setDataSet(dataSet);

        AgentConfig agent = new AgentConfig();
        agent.setName("${domain_name}分析助手");
        agent.setDescription("帮助您分析企业经营数据");
        agent.setEnableSearch(true);
        agent.setExamples(Arrays.asList("各行业的企业数量", "收入最高的品牌top10", "腾讯旗下的品牌有哪些", "按年份统计总收入趋势"));
        config.setAgent(agent);

        template.setTemplateConfig(config);
        semanticTemplateService.saveBuiltinTemplate(template, user);
        log.info("Initialized company template");
    }

    /**
     * Initialize Small Talk Semantic Template
     * 
     * @param user The user performing the initialization
     */
    private void initSmallTalkTemplate(User user) {
        SemanticTemplate template = new SemanticTemplate();
        template.setName("闲聊助手模板");
        template.setBizName("smalltalk_template");
        template.setCategory("CHAT");
        template.setDescription("直接与大模型对话，验证连通性。无需语义模型，仅创建闲聊Agent。");
        template.setStatus(1);

        SemanticTemplateConfig config = new SemanticTemplateConfig();
        // Agent-only template: no domain, no models, no dataSet, no terms

        AgentConfig agent = new AgentConfig();
        agent.setName("闲聊助手");
        agent.setDescription("直接与大模型对话，验证连通性");
        agent.setEnableSearch(false);
        agent.setExamples(Arrays.asList("如何才能变帅", "如何才能赚更多钱", "如何才能世界和平"));

        // Enable PlainText, disable S2SQL_SC
        Map<String, Boolean> overrides = new HashMap<>();
        overrides.put("PLAIN_TEXT", true);
        overrides.put("S2SQL_SC", false);
        agent.setChatAppOverrides(overrides);

        config.setAgent(agent);
        template.setTemplateConfig(config);
        semanticTemplateService.saveBuiltinTemplate(template, user);
        log.info("Initialized smalltalk template");
    }

    /**
     * Helper to create ConfigParam
     * 
     * @param key Parameter key
     * @param name Parameter name
     * @param type Parameter type
     * @param defaultValue Default value
     * @param required Whether required
     * @param description Parameter description
     * @return ConfigParam instance
     */
    private ConfigParam createParam(String key, String name, String type, String defaultValue,
            boolean required, String description) {
        ConfigParam param = new ConfigParam();
        param.setKey(key);
        param.setName(name);
        param.setType(type);
        param.setDefaultValue(defaultValue);
        param.setRequired(required);
        param.setDescription(description);
        return param;
    }

    /**
     * Helper to create ModelRelationConfig
     * 
     * @param fromBizName From model biz name
     * @param toBizName To model biz name
     * @param joinField Join field name
     * @return ModelRelationConfig instance
     */
    private ModelRelationConfig createRelation(String fromBizName, String toBizName,
            String joinField) {
        ModelRelationConfig relation = new ModelRelationConfig();
        relation.setFromModelBizName(fromBizName);
        relation.setToModelBizName(toBizName);
        relation.setJoinType("left join");

        JoinCondition condition = new JoinCondition();
        condition.setLeftField(joinField);
        condition.setRightField(joinField);
        condition.setOperator("EQUALS");
        relation.setJoinConditions(Collections.singletonList(condition));

        return relation;
    }
}
