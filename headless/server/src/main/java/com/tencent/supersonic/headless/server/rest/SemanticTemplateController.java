package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployParam;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployment;
import com.tencent.supersonic.headless.server.pojo.SemanticPreviewResult;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateListResp;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/semantic/v1")
@Slf4j
@RequiredArgsConstructor
public class SemanticTemplateController {

    private final SemanticTemplateService semanticTemplateService;

    /**
     * Get template list (builtin + custom)
     */
    @GetMapping("/templates")
    public SemanticTemplateListResp getTemplates(HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.getTemplateList(user);
    }

    /**
     * Get template by ID
     */
    @GetMapping("/templates/{id}")
    public SemanticTemplate getTemplateById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.getTemplateById(id, user);
    }

    /**
     * Create template
     */
    @PostMapping("/templates")
    public SemanticTemplate createTemplate(@RequestBody SemanticTemplate template,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.createTemplate(template, user);
    }

    /**
     * Update template (partial update)
     */
    @PatchMapping("/templates/{id}")
    public SemanticTemplate updateTemplate(@PathVariable Long id,
            @RequestBody SemanticTemplate template, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        template.setId(id);
        return semanticTemplateService.updateTemplate(template, user);
    }

    /**
     * Delete template
     */
    @DeleteMapping("/templates/{id}")
    public void deleteTemplate(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        semanticTemplateService.deleteTemplate(id, user);
    }

    /**
     * Preview deployment (custom action with colon)
     */
    @PostMapping("/templates/{id}:preview")
    public SemanticPreviewResult previewDeployment(@PathVariable Long id,
            @RequestBody SemanticDeployParam param, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.previewDeployment(id, param, user);
    }

    /**
     * Submit deployment asynchronously (custom action with colon). Returns immediately with PENDING
     * status. Use GET /deployments/{id} to poll.
     */
    @PostMapping("/templates/{id}:deploy")
    public SemanticDeployment submitDeployment(@PathVariable Long id,
            @RequestBody SemanticDeployParam param, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.submitDeployment(id, param, user);
    }

    /**
     * Get deployment list (current tenant)
     */
    @GetMapping("/deployments")
    public List<SemanticDeployment> getDeployments(HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.getDeploymentHistory(user);
    }

    /**
     * Get deployment by ID
     */
    @GetMapping("/deployments/{id}")
    public SemanticDeployment getDeploymentById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.getDeploymentById(id, user);
    }

    /**
     * Cancel a PENDING or RUNNING deployment
     */
    @PostMapping("/deployments/{id}:cancel")
    public SemanticDeployment cancelDeployment(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.cancelDeployment(id, user);
    }

    /**
     * Create/update builtin template (custom action with colon)
     */
    @PostMapping("/admin/templates:builtin")
    public SemanticTemplate saveBuiltinTemplate(@RequestBody SemanticTemplate template,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.saveBuiltinTemplate(template, user);
    }

    /**
     * Get all tenants' deployments (SuperAdmin only)
     */
    @GetMapping("/admin/deployments")
    public List<SemanticDeployment> getAllDeployments(HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticTemplateService.getAllDeploymentHistory(user);
    }
}
