import request from './request';

// ============ Types ============

export interface SemanticTemplate {
  id: number;
  name: string;
  bizName: string;
  description?: string;
  category: string;
  templateConfig: SemanticTemplateConfig;
  previewImage?: string;
  /**
   * Template status:
   * - 0: Draft (can edit/delete)
   * - 1: Deployed (cannot edit/delete)
   * - 2: Offline
   */
  status: number;
  currentVersion?: number;
  isBuiltin: boolean;
  tenantId: number;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

/**
 * Template status constants
 */
export const TEMPLATE_STATUS = {
  DRAFT: 0,
  DEPLOYED: 1,
  OFFLINE: 2,
} as const;

/**
 * Check if a template can be edited (only draft templates)
 */
export function canEditTemplate(template: SemanticTemplate): boolean {
  return !template.isBuiltin && template.status === TEMPLATE_STATUS.DRAFT;
}

/**
 * Check if a template can be deleted (only draft templates)
 */
export function canDeleteTemplate(template: SemanticTemplate): boolean {
  return !template.isBuiltin && template.status === TEMPLATE_STATUS.DRAFT;
}

export interface SemanticTemplateConfig {
  domain?: DomainConfig;
  models?: ModelConfig[];
  modelRelations?: ModelRelationConfig[];
  dataSet?: DataSetConfig;
  agent?: AgentConfig;
  terms?: TermConfig[];
  plugins?: PluginConfig[];
  configParams?: ConfigParam[];
}

export interface DomainConfig {
  name: string;
  bizName: string;
  description?: string;
  viewers?: string[];
  viewOrgs?: string[];
  admins?: string[];
  adminOrgs?: string[];
  isOpen?: number;
}

export interface ModelConfig {
  name: string;
  bizName: string;
  description?: string;
  tableName?: string;
  sqlQuery?: string;
  viewers?: string[];
  viewOrgs?: string[];
  admins?: string[];
  adminOrgs?: string[];
  identifiers?: IdentifyConfig[];
  dimensions?: DimensionConfig[];
  measures?: MeasureConfig[];
}

export interface IdentifyConfig {
  name: string;
  bizName: string;
  fieldName: string;
  type: string;
}

export interface DimensionConfig {
  name: string;
  bizName: string;
  fieldName?: string;
  type: string;
  expr?: string;
  dateFormat?: string;
  enableDictValue?: boolean;
}

export interface MeasureConfig {
  name: string;
  bizName: string;
  fieldName?: string;
  aggOperator: string;
  expr?: string;
  constraint?: string;
  createMetric?: boolean;
}

export interface ModelRelationConfig {
  fromModelBizName: string;
  toModelBizName: string;
  joinType: string;
  joinConditions?: JoinCondition[];
}

export interface JoinCondition {
  leftField: string;
  rightField: string;
  operator?: string;
}

export interface DataSetConfig {
  name: string;
  bizName: string;
  description?: string;
  admins?: string[];
  adminOrgs?: string[];
  viewers?: string[];
  viewOrgs?: string[];
  isOpen?: number;
}

export interface AgentConfig {
  name: string;
  description?: string;
  enableSearch?: boolean;
  examples?: string[];
  admins?: string[];
  viewers?: string[];
}

export interface TermConfig {
  name: string;
  description?: string;
  alias?: string[];
}

export interface PluginConfig {
  type: string;
  name: string;
  pattern?: string;
  config?: any;
}

export interface ConfigParam {
  key: string;
  name: string;
  type: string;
  defaultValue?: string;
  required: boolean;
  description?: string;
}

export interface SemanticDeployParam {
  databaseId: number;
  allowRedeploy?: boolean;
  params: Record<string, string>;
}

export interface SemanticPreviewResult {
  domain?: any;
  models?: any[];
  metrics?: any[];
  dimensions?: any[];
  dataSet?: any;
  agent?: AgentPreview;
  terms?: TermPreview[];
}

export interface AgentPreview {
  name: string;
  description?: string;
  examples?: string[];
}

export interface TermPreview {
  name: string;
  description?: string;
  alias?: string[];
}

export interface SemanticDeployment {
  id: number;
  templateId: number;
  templateName?: string;
  templateVersion?: number;
  templateConfigSnapshot?: SemanticTemplateConfig;
  databaseId?: number;
  paramConfig?: SemanticDeployParam;
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
  resultDetail?: SemanticDeployResult;
  errorMessage?: string;
  currentStep?: string;
  startTime?: string;
  endTime?: string;
  tenantId: number;
  createdAt?: string;
  createdBy?: string;
}

export interface SemanticDeployResult {
  domainId?: number;
  domainName?: string;
  models?: CreatedModel[];
  metrics?: CreatedMetric[];
  dimensions?: CreatedDimension[];
  dataSetId?: number;
  dataSetName?: string;
  /**
   * Agent configuration for creating Agent through chat module.
   * Agent is not created by headless module to keep modules independent.
   */
  agentConfig?: AgentConfigResult;
  terms?: CreatedTerm[];
}

/**
 * Agent configuration that can be used to create an Agent through the chat module.
 */
export interface AgentConfigResult {
  name?: string;
  description?: string;
  enableSearch?: boolean;
  examples?: string[];
  dataSetId?: number;
  dataSetName?: string;
}

export interface CreatedModel {
  id: number;
  name: string;
  bizName: string;
}

export interface CreatedMetric {
  id: number;
  name: string;
  bizName: string;
  modelId: number;
}

export interface CreatedDimension {
  id: number;
  name: string;
  bizName: string;
  modelId: number;
}

export interface CreatedTerm {
  id: number;
  name: string;
}

/**
 * Response for template list, separating builtin and custom templates
 */
export interface SemanticTemplateListResp {
  builtinTemplates: SemanticTemplate[];
  customTemplates: SemanticTemplate[];
}

// ============ API Base Paths (RESTful / Google Style) ============

const API_V1 = '/api/semantic/v1';
const TEMPLATES_BASE = `${API_V1}/templates`;
const DEPLOYMENTS_BASE = `${API_V1}/deployments`;
const ADMIN_BASE = `${API_V1}/admin`;

// ============ Templates Resource ============

/**
 * Get template list (builtin + custom)
 */
export function getTemplateList(): Promise<SemanticTemplateListResp> {
  return request(TEMPLATES_BASE, {
    method: 'GET',
  });
}

/**
 * Get template by ID
 */
export function getTemplateById(id: number): Promise<SemanticTemplate> {
  return request(`${TEMPLATES_BASE}/${id}`, {
    method: 'GET',
  });
}

/**
 * Create template
 */
export function createTemplate(template: Partial<SemanticTemplate>): Promise<SemanticTemplate> {
  return request(TEMPLATES_BASE, {
    method: 'POST',
    data: template,
  });
}

/**
 * Update template (partial update)
 */
export function updateTemplate(
  id: number,
  template: Partial<SemanticTemplate>,
): Promise<SemanticTemplate> {
  return request(`${TEMPLATES_BASE}/${id}`, {
    method: 'PATCH',
    data: template,
  });
}

/**
 * Delete template
 */
export function deleteTemplate(id: number): Promise<void> {
  return request(`${TEMPLATES_BASE}/${id}`, {
    method: 'DELETE',
  });
}

/**
 * Preview deployment (custom action with colon)
 */
export function previewDeployment(
  templateId: number,
  param: SemanticDeployParam,
): Promise<SemanticPreviewResult> {
  return request(`${TEMPLATES_BASE}/${templateId}:preview`, {
    method: 'POST',
    data: param,
  });
}

/**
 * Execute deployment (custom action with colon)
 */
export function executeDeployment(
  templateId: number,
  param: SemanticDeployParam,
): Promise<SemanticDeployment> {
  return request(`${TEMPLATES_BASE}/${templateId}:deploy`, {
    method: 'POST',
    data: param,
  });
}

// ============ Deployments Resource ============

/**
 * Get deployment list (current tenant)
 */
export function getDeploymentHistory(): Promise<SemanticDeployment[]> {
  return request(DEPLOYMENTS_BASE, {
    method: 'GET',
  });
}

/**
 * Get deployment by ID
 */
export function getDeploymentById(id: number): Promise<SemanticDeployment> {
  return request(`${DEPLOYMENTS_BASE}/${id}`, {
    method: 'GET',
  });
}

/**
 * Cancel a PENDING or RUNNING deployment
 */
export function cancelDeployment(id: number): Promise<SemanticDeployment> {
  return request(`${DEPLOYMENTS_BASE}/${id}:cancel`, {
    method: 'POST',
  });
}

// ============ Admin APIs (SuperAdmin only) ============

/**
 * Create/update builtin template (custom action with colon)
 */
export function saveBuiltinTemplate(template: Partial<SemanticTemplate>): Promise<SemanticTemplate> {
  return request(`${ADMIN_BASE}/templates:builtin`, {
    method: 'POST',
    data: template,
  });
}

/**
 * Get all tenants' deployments (SuperAdmin only)
 */
export function getAllDeploymentHistory(): Promise<SemanticDeployment[]> {
  return request(`${ADMIN_BASE}/deployments`, {
    method: 'GET',
  });
}
