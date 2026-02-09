package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployParam;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployment;
import com.tencent.supersonic.headless.server.pojo.SemanticPreviewResult;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateListResp;

import java.util.List;

public interface SemanticTemplateService {

    /**
     * Get templates available to the current tenant, separated into builtin and custom lists.
     */
    SemanticTemplateListResp getTemplateList(User user);

    /**
     * Get template by ID (checks tenant permission).
     */
    SemanticTemplate getTemplateById(Long id, User user);

    /**
     * Create a tenant-specific custom template. Automatically sets tenant_id to current tenant.
     */
    SemanticTemplate createTemplate(SemanticTemplate template, User user);

    /**
     * Update template (only allows updating tenant's own templates, builtin templates require SaaS
     * admin permission).
     */
    SemanticTemplate updateTemplate(SemanticTemplate template, User user);

    /**
     * Delete template (only allows deleting tenant's own templates).
     */
    void deleteTemplate(Long id, User user);

    /**
     * Preview deployment. Checks template access permission, returns objects to be created.
     */
    SemanticPreviewResult previewDeployment(Long templateId, SemanticDeployParam param, User user);

    /**
     * Submit deployment asynchronously. Returns immediately with PENDING status. Use
     * getDeploymentById() to poll for completion.
     */
    SemanticDeployment submitDeployment(Long templateId, SemanticDeployParam param, User user);

    /**
     * Execute deployment synchronously. All created objects automatically belong to the current
     * tenant. Used by BuiltinSemanticTemplateInitializer at startup.
     */
    SemanticDeployment executeDeployment(Long templateId, SemanticDeployParam param, User user);

    /**
     * Get deployment history for the current tenant.
     */
    List<SemanticDeployment> getDeploymentHistory(User user);

    /**
     * Get deployment detail by ID (checks tenant permission).
     */
    SemanticDeployment getDeploymentById(Long id, User user);

    /**
     * Cancel a PENDING or RUNNING deployment. Triggers rollback of any partially created objects.
     */
    SemanticDeployment cancelDeployment(Long deploymentId, User user);

    /**
     * Get all builtin templates.
     */
    List<SemanticTemplate> getBuiltinTemplates();

    /**
     * Get all deployment history across tenants (SaaS Admin).
     */
    List<SemanticDeployment> getAllDeploymentHistory(User user);

    /**
     * Create/update builtin template (SaaS Admin).
     */
    SemanticTemplate saveBuiltinTemplate(SemanticTemplate template, User user);
}
