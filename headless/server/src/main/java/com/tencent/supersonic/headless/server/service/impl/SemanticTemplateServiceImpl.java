package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.server.event.TemplateDeployedEvent;
import com.tencent.supersonic.headless.server.executor.SemanticDeployExecutor;
import com.tencent.supersonic.headless.server.persistence.dataobject.SemanticDeploymentDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.SemanticTemplateDO;
import com.tencent.supersonic.headless.server.persistence.mapper.SemanticDeploymentMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.SemanticTemplateMapper;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployParam;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployment;
import com.tencent.supersonic.headless.server.pojo.SemanticPreviewResult;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateListResp;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SemanticTemplateServiceImpl extends
        ServiceImpl<SemanticTemplateMapper, SemanticTemplateDO> implements SemanticTemplateService {

    private static final Integer STATUS_DRAFT = 0;
    private static final Integer STATUS_DEPLOYED = 1;

    private final Map<Long, Future<?>> runningDeployments = new ConcurrentHashMap<>();

    private final SemanticDeploymentMapper deploymentMapper;
    private final SemanticDeployExecutor deployExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TenantConfig tenantConfig;
    private final ThreadPoolExecutor deployPool;
    private final DomainService domainService;

    public SemanticTemplateServiceImpl(SemanticDeploymentMapper deploymentMapper,
            @Lazy SemanticDeployExecutor deployExecutor,
            ApplicationEventPublisher applicationEventPublisher, TenantConfig tenantConfig,
            @Qualifier("deployExecutor") ThreadPoolExecutor deployPool,
            DomainService domainService) {
        this.deploymentMapper = deploymentMapper;
        this.deployExecutor = deployExecutor;
        this.applicationEventPublisher = applicationEventPublisher;
        this.tenantConfig = tenantConfig;
        this.deployPool = deployPool;
        this.domainService = domainService;
    }

    @Override
    public SemanticTemplateListResp getTemplateList(User user) {
        Long tenantId = getTenantId(user);

        QueryWrapper<SemanticTemplateDO> builtinWrapper = new QueryWrapper<>();
        builtinWrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 1)
                .orderByDesc(SemanticTemplateDO::getCreatedAt);
        List<SemanticTemplateDO> builtinDOs = baseMapper.selectList(builtinWrapper);
        List<SemanticTemplate> builtinTemplates =
                builtinDOs.stream().map(this::convertToTemplate).collect(Collectors.toList());

        QueryWrapper<SemanticTemplateDO> customWrapper = new QueryWrapper<>();
        customWrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 0)
                .eq(SemanticTemplateDO::getTenantId, tenantId)
                .orderByDesc(SemanticTemplateDO::getCreatedAt);
        List<SemanticTemplateDO> customDOs = baseMapper.selectList(customWrapper);
        List<SemanticTemplate> customTemplates =
                customDOs.stream().map(this::convertToTemplate).collect(Collectors.toList());
        return new SemanticTemplateListResp(builtinTemplates, customTemplates);
    }

    @Override
    public SemanticTemplate getTemplateById(Long id, User user) {
        SemanticTemplateDO templateDO = baseMapper.selectById(id);
        if (templateDO == null) {
            throw new InvalidArgumentException("Template not found: " + id);
        }
        Long tenantId = getTenantId(user);
        boolean isBuiltin = templateDO.getIsBuiltin() != null && templateDO.getIsBuiltin() == 1;
        if (!isBuiltin && !templateDO.getTenantId().equals(tenantId)) {
            throw new InvalidArgumentException("No permission to access this template");
        }
        return convertToTemplate(templateDO);
    }

    @Override
    @Transactional
    public SemanticTemplate createTemplate(SemanticTemplate template, User user) {
        Long tenantId = getTenantId(user);
        template.setTenantId(tenantId);
        template.setIsBuiltin(false);
        template.setCreatedBy(user.getName());
        template.setCreatedAt(new Date());
        template.setStatus(STATUS_DRAFT);

        SemanticTemplateDO templateDO = convertToDO(template);
        baseMapper.insert(templateDO);
        template.setId(templateDO.getId());
        return template;
    }

    @Override
    @Transactional
    public SemanticTemplate updateTemplate(SemanticTemplate template, User user) {
        SemanticTemplateDO existingDO = baseMapper.selectById(template.getId());
        if (existingDO == null) {
            throw new InvalidArgumentException("Template not found: " + template.getId());
        }

        Long tenantId = getTenantId(user);
        if (existingDO.getIsBuiltin() == 1) {
            if (!user.isSuperAdmin()) {
                throw new InvalidArgumentException("Only SaaS admin can update builtin templates");
            }
        } else {
            if (!existingDO.getTenantId().equals(tenantId)) {
                throw new InvalidArgumentException("No permission to update this template");
            }
            if (!STATUS_DRAFT.equals(existingDO.getStatus())) {
                throw new InvalidArgumentException("Cannot edit template that has been deployed");
            }
        }

        template.setUpdatedBy(user.getName());
        template.setUpdatedAt(new Date());
        SemanticTemplateDO templateDO = convertToDO(template);
        templateDO.setId(existingDO.getId());
        templateDO.setTenantId(existingDO.getTenantId());
        templateDO.setIsBuiltin(existingDO.getIsBuiltin());
        templateDO.setStatus(existingDO.getStatus());
        baseMapper.updateById(templateDO);
        return convertToTemplate(templateDO);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id, User user) {
        SemanticTemplateDO templateDO = baseMapper.selectById(id);
        if (templateDO == null) {
            return;
        }

        Long tenantId = getTenantId(user);
        if (templateDO.getIsBuiltin() == 1) {
            throw new InvalidArgumentException("Cannot delete builtin templates");
        }
        if (!templateDO.getTenantId().equals(tenantId)) {
            throw new InvalidArgumentException("No permission to delete this template");
        }
        if (!STATUS_DRAFT.equals(templateDO.getStatus())) {
            throw new InvalidArgumentException("Cannot delete template that has been deployed");
        }

        baseMapper.deleteById(id);
    }

    @Override
    public SemanticPreviewResult previewDeployment(Long templateId, SemanticDeployParam param,
            User user) {
        SemanticTemplate template = getTemplateById(templateId, user);
        return deployExecutor.preview(template, param, user);
    }

    @Override
    public SemanticDeployment submitDeployment(Long templateId, SemanticDeployParam param,
            User user) {
        SemanticTemplate template = getTemplateById(templateId, user);
        Long tenantId = getTenantId(user);

        if (!param.isAllowRedeploy() && isDeploymentStillActive(templateId, tenantId)) {
            throw new InvalidArgumentException("该模板已成功部署且语义对象仍然存在。如需重新部署，请勾选「允许重新部署」。");
        }

        // Synchronous pre-deployment validation (fails fast before creating record)
        deployExecutor.validate(template, param, user);

        SemanticDeployment deployment = new SemanticDeployment();
        deployment.setTemplateId(templateId);
        deployment.setTemplateName(template.getName());
        deployment.setDatabaseId(param.getDatabaseId());
        deployment.setParamConfig(param);
        deployment.setStatus(SemanticDeployment.DeploymentStatus.PENDING);
        deployment.setTenantId(tenantId);
        deployment.setCreatedBy(user.getName());
        deployment.setCreatedAt(new Date());

        SemanticDeploymentDO deploymentDO = convertToDeploymentDO(deployment);
        try {
            deploymentMapper.insert(deploymentDO);
        } catch (DuplicateKeyException e) {
            throw new InvalidArgumentException("该模板已有正在执行的部署任务，请等待完成后再试。");
        }
        deployment.setId(deploymentDO.getId());

        // Submit async execution on dedicated deploy thread pool
        final Long capturedTenantId = tenantId;
        final Long deploymentId = deployment.getId();
        Future<?> future = deployPool.submit(() -> {
            try {
                executeDeploymentAsync(deploymentId, template, param, user, capturedTenantId);
            } finally {
                runningDeployments.remove(deploymentId);
            }
        });
        runningDeployments.put(deploymentId, future);

        return deployment;
    }

    private void executeDeploymentAsync(Long deploymentId, SemanticTemplate template,
            SemanticDeployParam param, User user, Long tenantId) {
        try {
            TenantContext.setTenantId(tenantId);

            SemanticDeploymentDO deploymentDO = deploymentMapper.selectById(deploymentId);
            if (deploymentDO == null) {
                log.error("Deployment record not found for async execution: {}", deploymentId);
                return;
            }

            SemanticDeployment deployment = convertToDeployment(deploymentDO);
            deployment.setStatus(SemanticDeployment.DeploymentStatus.RUNNING);
            deployment.setStartTime(new Date());
            updateDeploymentStatus(deployment);

            try {
                SemanticDeployResult result = deployExecutor.execute(template, param, user,
                        step -> updateDeploymentStep(deployment, step));

                applicationEventPublisher.publishEvent(new TemplateDeployedEvent(this, result,
                        template.getTemplateConfig(), user));

                deployment.setResultDetail(result);
                deployment.setStatus(SemanticDeployment.DeploymentStatus.SUCCESS);
                deployment.setEndTime(new Date());

                SemanticTemplateDO templateDO = baseMapper.selectById(template.getId());
                if (templateDO != null && templateDO.getIsBuiltin() == 0) {
                    templateDO.setStatus(STATUS_DEPLOYED);
                    templateDO.setUpdatedAt(new Date());
                    templateDO.setUpdatedBy(user.getName());
                    baseMapper.updateById(templateDO);
                    log.info("Template {} status updated to DEPLOYED after successful deployment",
                            template.getId());
                }
            } catch (Exception e) {
                // Re-read status in case cancelDeployment() already set it to CANCELLED
                SemanticDeploymentDO latestDO = deploymentMapper.selectById(deploymentId);
                if (latestDO != null && SemanticDeployment.DeploymentStatus.CANCELLED.name()
                        .equals(latestDO.getStatus())) {
                    log.info("Deployment {} was cancelled, skipping FAILED update", deploymentId);
                    return;
                }
                log.error("Failed to deploy template: {}", template.getId(), e);
                deployment.setStatus(SemanticDeployment.DeploymentStatus.FAILED);
                deployment.setErrorMessage(e.getMessage());
                deployment.setEndTime(new Date());
            }

            updateDeploymentStatus(deployment);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public SemanticDeployment executeDeployment(Long templateId, SemanticDeployParam param,
            User user) {
        SemanticTemplate template = getTemplateById(templateId, user);
        Long tenantId = getTenantId(user);

        if (!param.isAllowRedeploy() && isDeploymentStillActive(templateId, tenantId)) {
            throw new InvalidArgumentException("该模板已成功部署且语义对象仍然存在。如需重新部署，请勾选「允许重新部署」。");
        }

        deployExecutor.validate(template, param, user);

        SemanticDeployment deployment = new SemanticDeployment();
        deployment.setTemplateId(templateId);
        deployment.setTemplateName(template.getName());
        deployment.setDatabaseId(param.getDatabaseId());
        deployment.setParamConfig(param);
        deployment.setStatus(SemanticDeployment.DeploymentStatus.PENDING);
        deployment.setTenantId(tenantId);
        deployment.setCreatedBy(user.getName());
        deployment.setCreatedAt(new Date());

        SemanticDeploymentDO deploymentDO = convertToDeploymentDO(deployment);
        try {
            deploymentMapper.insert(deploymentDO);
        } catch (DuplicateKeyException e) {
            throw new InvalidArgumentException("该模板已有正在执行的部署任务，请等待完成后再试。");
        }
        deployment.setId(deploymentDO.getId());

        try {
            deployment.setStatus(SemanticDeployment.DeploymentStatus.RUNNING);
            deployment.setStartTime(new Date());
            updateDeploymentStatus(deployment);

            SemanticDeployResult result = deployExecutor.execute(template, param, user,
                    step -> updateDeploymentStep(deployment, step));

            applicationEventPublisher.publishEvent(
                    new TemplateDeployedEvent(this, result, template.getTemplateConfig(), user));

            deployment.setResultDetail(result);
            deployment.setStatus(SemanticDeployment.DeploymentStatus.SUCCESS);
            deployment.setEndTime(new Date());

            SemanticTemplateDO templateDO = baseMapper.selectById(templateId);
            if (templateDO != null && templateDO.getIsBuiltin() == 0) {
                templateDO.setStatus(STATUS_DEPLOYED);
                templateDO.setUpdatedAt(new Date());
                templateDO.setUpdatedBy(user.getName());
                baseMapper.updateById(templateDO);
                log.info("Template {} status updated to DEPLOYED after successful deployment",
                        templateId);
            }
        } catch (Exception e) {
            log.error("Failed to deploy template: {}", templateId, e);
            deployment.setStatus(SemanticDeployment.DeploymentStatus.FAILED);
            deployment.setErrorMessage(e.getMessage());
            deployment.setEndTime(new Date());
        }

        updateDeploymentStatus(deployment);
        return deployment;
    }

    private void updateDeploymentStatus(SemanticDeployment deployment) {
        SemanticDeploymentDO deploymentDO = convertToDeploymentDO(deployment);
        deploymentMapper.updateById(deploymentDO);
    }

    private void updateDeploymentStep(SemanticDeployment deployment, String step) {
        deployment.setCurrentStep(step);
        SemanticDeploymentDO deploymentDO = new SemanticDeploymentDO();
        deploymentDO.setId(deployment.getId());
        deploymentDO.setCurrentStep(step);
        deploymentMapper.updateById(deploymentDO);
    }

    @Override
    public List<SemanticDeployment> getDeploymentHistory(User user) {
        Long tenantId = getTenantId(user);
        QueryWrapper<SemanticDeploymentDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticDeploymentDO::getTenantId, tenantId)
                .orderByDesc(SemanticDeploymentDO::getCreatedAt);
        List<SemanticDeploymentDO> deploymentDOs = deploymentMapper.selectList(wrapper);
        return deploymentDOs.stream().map(this::convertToDeployment).collect(Collectors.toList());
    }

    @Override
    public SemanticDeployment getDeploymentById(Long id, User user) {
        SemanticDeploymentDO deploymentDO = deploymentMapper.selectById(id);
        if (deploymentDO == null) {
            throw new InvalidArgumentException("Deployment not found: " + id);
        }

        Long tenantId = getTenantId(user);
        if (!deploymentDO.getTenantId().equals(tenantId) && !user.isSuperAdmin()) {
            throw new InvalidArgumentException("No permission to access this deployment");
        }

        return convertToDeployment(deploymentDO);
    }

    @Override
    public SemanticDeployment cancelDeployment(Long deploymentId, User user) {
        SemanticDeploymentDO deploymentDO = deploymentMapper.selectById(deploymentId);
        if (deploymentDO == null) {
            throw new InvalidArgumentException("Deployment not found: " + deploymentId);
        }

        Long tenantId = getTenantId(user);
        if (!deploymentDO.getTenantId().equals(tenantId) && !user.isSuperAdmin()) {
            throw new InvalidArgumentException("No permission to cancel this deployment");
        }

        SemanticDeployment.DeploymentStatus status =
                SemanticDeployment.DeploymentStatus.valueOf(deploymentDO.getStatus());
        if (status != SemanticDeployment.DeploymentStatus.PENDING
                && status != SemanticDeployment.DeploymentStatus.RUNNING) {
            throw new InvalidArgumentException("只能取消 PENDING 或 RUNNING 状态的部署");
        }

        // Interrupt the async thread if running
        Future<?> future = runningDeployments.remove(deploymentId);
        if (future != null) {
            future.cancel(true);
        }

        SemanticDeployment deployment = convertToDeployment(deploymentDO);
        deployment.setStatus(SemanticDeployment.DeploymentStatus.CANCELLED);
        deployment.setErrorMessage("用户取消部署");
        deployment.setEndTime(new Date());
        updateDeploymentStatus(deployment);

        log.info("Deployment {} cancelled by user {}", deploymentId, user.getName());
        return deployment;
    }

    @Override
    public List<SemanticTemplate> getBuiltinTemplates() {
        QueryWrapper<SemanticTemplateDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 1);
        List<SemanticTemplateDO> templateDOs = baseMapper.selectList(wrapper);
        return templateDOs.stream().map(this::convertToTemplate).collect(Collectors.toList());
    }

    @Override
    public List<SemanticDeployment> getAllDeploymentHistory(User user) {
        if (!user.isSuperAdmin()) {
            throw new InvalidArgumentException("Only SaaS admin can view all deployment history");
        }

        QueryWrapper<SemanticDeploymentDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().orderByDesc(SemanticDeploymentDO::getCreatedAt);
        List<SemanticDeploymentDO> deploymentDOs = deploymentMapper.selectList(wrapper);
        return deploymentDOs.stream().map(this::convertToDeployment).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SemanticTemplate saveBuiltinTemplate(SemanticTemplate template, User user) {
        if (!user.isSuperAdmin()) {
            throw new InvalidArgumentException("Only SaaS admin can manage builtin templates");
        }

        template.setTenantId(tenantConfig.getDefaultTenantId());
        template.setIsBuiltin(true);
        template.setStatus(STATUS_DEPLOYED);

        QueryWrapper<SemanticTemplateDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticTemplateDO::getTenantId, tenantConfig.getDefaultTenantId())
                .eq(SemanticTemplateDO::getBizName, template.getBizName());
        SemanticTemplateDO existingDO = baseMapper.selectOne(wrapper);

        if (existingDO != null) {
            template.setId(existingDO.getId());
            template.setUpdatedBy(user.getName());
            template.setUpdatedAt(new Date());
            SemanticTemplateDO templateDO = convertToDO(template);
            baseMapper.updateById(templateDO);
        } else {
            template.setCreatedBy(user.getName());
            template.setCreatedAt(new Date());
            SemanticTemplateDO templateDO = convertToDO(template);
            baseMapper.insert(templateDO);
            template.setId(templateDO.getId());
        }

        return template;
    }

    // ============ Helper Methods ============

    private Long getTenantId(User user) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        if (user.getTenantId() != null) {
            return user.getTenantId();
        }
        return tenantConfig.getDefaultTenantId();
    }

    /**
     * Check if a previous deployment is still active (domain still exists). Returns false if no
     * successful deployment exists or if the deployed domain has been deleted.
     */
    private boolean isDeploymentStillActive(Long templateId, Long tenantId) {
        QueryWrapper<SemanticDeploymentDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticDeploymentDO::getTemplateId, templateId)
                .eq(SemanticDeploymentDO::getTenantId, tenantId)
                .eq(SemanticDeploymentDO::getStatus,
                        SemanticDeployment.DeploymentStatus.SUCCESS.name())
                .orderByDesc(SemanticDeploymentDO::getCreatedAt).last("LIMIT 1");
        SemanticDeploymentDO lastSuccess = deploymentMapper.selectOne(wrapper);

        if (lastSuccess == null) {
            return false;
        }

        // Check if the deployed domain still exists
        SemanticDeployment deployment = convertToDeployment(lastSuccess);
        SemanticDeployResult result = deployment.getResultDetail();
        if (result != null && result.getDomainId() != null) {
            DomainResp domain = domainService.getDomain(result.getDomainId());
            if (domain == null) {
                log.info(
                        "Previous deployment domain {} has been deleted, allowing redeploy "
                                + "for template {} in tenant {}",
                        result.getDomainId(), templateId, tenantId);
                return false;
            }
        }

        return true;
    }

    private SemanticTemplate convertToTemplate(SemanticTemplateDO templateDO) {
        if (templateDO == null) {
            return null;
        }
        SemanticTemplate template = new SemanticTemplate();
        BeanUtils.copyProperties(templateDO, template, "isBuiltin", "templateConfig");
        template.setIsBuiltin(templateDO.getIsBuiltin() != null && templateDO.getIsBuiltin() == 1);
        if (StringUtils.isNotBlank(templateDO.getTemplateConfig())) {
            template.setTemplateConfig(JsonUtil.toObject(templateDO.getTemplateConfig(),
                    SemanticTemplateConfig.class));
        }
        return template;
    }

    private SemanticTemplateDO convertToDO(SemanticTemplate template) {
        SemanticTemplateDO templateDO = new SemanticTemplateDO();
        BeanUtils.copyProperties(template, templateDO, "isBuiltin", "templateConfig");
        templateDO.setIsBuiltin(template.getIsBuiltin() != null && template.getIsBuiltin() ? 1 : 0);
        if (template.getTemplateConfig() != null) {
            templateDO.setTemplateConfig(JsonUtil.toString(template.getTemplateConfig()));
        }
        return templateDO;
    }

    private SemanticDeployment convertToDeployment(SemanticDeploymentDO deploymentDO) {
        if (deploymentDO == null) {
            return null;
        }
        SemanticDeployment deployment = new SemanticDeployment();
        BeanUtils.copyProperties(deploymentDO, deployment, "status", "paramConfig", "resultDetail");
        deployment.setStatus(SemanticDeployment.DeploymentStatus.valueOf(deploymentDO.getStatus()));
        if (StringUtils.isNotBlank(deploymentDO.getParamConfig())) {
            deployment.setParamConfig(
                    JsonUtil.toObject(deploymentDO.getParamConfig(), SemanticDeployParam.class));
        }
        if (StringUtils.isNotBlank(deploymentDO.getResultDetail())) {
            deployment.setResultDetail(
                    JsonUtil.toObject(deploymentDO.getResultDetail(), SemanticDeployResult.class));
        }
        return deployment;
    }

    private SemanticDeploymentDO convertToDeploymentDO(SemanticDeployment deployment) {
        SemanticDeploymentDO deploymentDO = new SemanticDeploymentDO();
        BeanUtils.copyProperties(deployment, deploymentDO, "status", "paramConfig", "resultDetail",
                "activeLock");
        deploymentDO.setStatus(deployment.getStatus().name());
        if (deployment.getParamConfig() != null) {
            deploymentDO.setParamConfig(JsonUtil.toString(deployment.getParamConfig()));
        }
        if (deployment.getResultDetail() != null) {
            deploymentDO.setResultDetail(JsonUtil.toString(deployment.getResultDetail()));
        }
        // activeLock is non-null only for PENDING/RUNNING, enabling DB unique constraint
        SemanticDeployment.DeploymentStatus status = deployment.getStatus();
        if (status == SemanticDeployment.DeploymentStatus.PENDING
                || status == SemanticDeployment.DeploymentStatus.RUNNING) {
            deploymentDO.setActiveLock(deployment.getTemplateId() + "_" + deployment.getTenantId());
        } else {
            deploymentDO.setActiveLock(null);
        }
        return deploymentDO;
    }
}
