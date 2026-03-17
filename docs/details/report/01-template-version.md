---
status: implemented
module: headless/server
key-files:
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/SemanticTemplateServiceImpl.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/SemanticDeployment.java
  - webapp/packages/supersonic-fe/src/pages/SemanticModel/SemanticTemplate/components/DeployHistory.tsx
depends-on: []
---

# 模板与版本管理

## 目标

保证模板的每次编辑可追溯、部署快照不可变，调度任务和执行记录都能绑定到创建时刻的模板版本，为历史回溯和成本分析提供基础。

## 当前状态

已上线。版本号字段、部署快照、模板下线状态已通过 V19 迁移脚本落库，相关 Service、Controller、前端页面均已实现。

## 设计决策

采用**"主表版本号 + 部署快照"**模式，避免独立版本表带来的额外 JOIN 复杂度。

- 模板每次编辑 → `current_version++`（版本号单调递增，初始值=1）
- 部署时将当前版本号和配置快照写入 `s2_semantic_deployment`（不可变）
- 调度任务绑定的 `templateVersion` 指向创建/更新时刻的版本号
- 只允许最新版本部署（不支持老版本重新部署）
- 版本查询直接查 `s2_semantic_deployment` 表，部署记录天然即版本历史，无需新表

版本与调度的关系：
```
模板 V1 → 部署 → 创建 DataSet + Agent
                    ↓
              用户创建调度任务 → schedule.templateVersion = 1

模板编辑 → V2
模板重新部署 → 更新 DataSet（结构可能变化）

调度任务执行时：
  if (schedule.templateVersion < template.currentVersion) {
      log.warn("模板已更新，调度任务绑定的版本可能过时");
      // 不阻断执行，但在执行记录中标注版本差异
  }
```

## 接口契约

```
GET /api/semantic/template/{id}/versions
    → 查询 s2_semantic_deployment WHERE template_id = {id}
      ORDER BY template_version DESC
    → 返回：[{version, deployTime, deployedBy, status, configSnapshot}]

GET /api/semantic/template/{id}/versions/{version}
    → 返回特定版本的配置快照（用于对比差异）

POST /templates/{id}:offline
    → 将模板状态设为 Offline(2)，下线前校验活跃调度任务
```

## 数据模型

```sql
-- V19__template_version.sql (MySQL)

-- 1. 模板主表增加版本号
ALTER TABLE s2_semantic_template
  ADD COLUMN `current_version` bigint NOT NULL DEFAULT 1
    COMMENT '当前版本号，每次编辑+1';

-- 2. 部署记录表增加版本快照
ALTER TABLE s2_semantic_deployment
  ADD COLUMN `template_version` bigint DEFAULT NULL
    COMMENT '部署时的模板版本号',
  ADD COLUMN `template_config_snapshot` longtext DEFAULT NULL
    COMMENT 'JSON: 部署时刻的模板配置快照（不可变，用于回溯）';
```

状态流转：
```
Draft(0) → Deployed(1) → Offline(2)
                ↑              │
                └──────────────┘ （重新部署可回到 Deployed）
```

模板元数据字段：

**语义模板层**（`s2_semantic_template`）：

| 字段 | 对应现有字段 | 说明 |
|------|-----------|------|
| template_code | `biz_name` | 模板唯一编码 |
| template_name | `name` | 模板名称 |
| business_domain | `template_config → DomainConfig.name` | 业务域 |
| datasource_type | `template_config → ConfigParam[type=DATABASE]` | 数据源由部署参数指定 |
| status | `status` | 0=Draft, 1=Deployed, 2=Offline |
| creator | `created_by` | 创建人 |
| current_version | `current_version` | 当前生效版本号，模板每次编辑+1 |

**数据集层**（`s2_data_set`）— 模板部署后生成：

| 字段 | 说明 |
|------|------|
| `data_set_detail` | JSON：关联的 Model、Metric、Dimension 集合 |
| `query_config` | JSON：默认查询配置（时间范围、聚合方式） |

**部署时参数**（`ConfigParam`）：

| 字段 | 对应现有字段 | 说明 |
|------|-----------|------|
| param_key | `ConfigParam.key` | 参数名 |
| param_type | `ConfigParam.type` | DATABASE / TABLE / FIELD / TEXT |
| required | 隐式（模板引用即必填） | `${key}` 占位符在部署时解析 |
| default_value | `ConfigParam.defaultValue` | 默认值 |

## 实现要点

核心逻辑变更：

```java
// SemanticTemplateServiceImpl.updateTemplate() — 版本递增
@Override
@Transactional
public SemanticTemplate updateTemplate(SemanticTemplate template, User user) {
    SemanticTemplateDO existingDO = baseMapper.selectById(template.getId());
    Long newVersion = (existingDO.getCurrentVersion() != null
        ? existingDO.getCurrentVersion() : 0L) + 1;
    template.setCurrentVersion(newVersion);
    // ... updateById（已有）
}

// SemanticDeployExecutor.execute() — 部署时写入版本快照
deploymentDO.setTemplateVersion(templateDO.getCurrentVersion());
deploymentDO.setTemplateConfigSnapshot(templateDO.getTemplateConfig());
```

实现映射：

| 设计概念 | 现有实现 | 状态 |
|---------|---------|------|
| 模板 CRUD | `SemanticTemplateServiceImpl` | ✅ 已实现 |
| 状态流转 | `SemanticTemplateDO.status` | ✅ 已实现完整生命周期 |
| 模板分类 | `SemanticTemplateConfig.DomainConfig` | ✅ 按 Domain 分类 |
| 模板部署 | `SemanticDeployExecutor.execute()` | ✅ 一键创建 Domain → Model → Metric → DataSet → Agent |
| 模板版本管理 | `SemanticTemplateDO.currentVersion` + `SemanticDeploymentDO.templateVersion/templateConfigSnapshot` | ✅ 已实现 |

影响文件：

| 文件 | 变更 | 状态 |
|------|------|------|
| `SemanticTemplateDO.java` | 新增 `currentVersion` 字段 | ✅ 已实现 |
| `SemanticTemplate.java` | 新增 `currentVersion` 字段 | ✅ 已实现 |
| `SemanticDeploymentDO.java` | 新增 `templateVersion` + `templateConfigSnapshot` 字段 | ✅ 已实现 |
| `SemanticDeployment.java` | 新增 `templateVersion`（Long）+ `templateConfigSnapshot`（SemanticTemplateConfig） | ✅ 已实现 |
| `SemanticTemplateServiceImpl.java` | 版本递增、版本快照写入 | ✅ 已实现 |
| `V19__template_version.sql`（MySQL + PostgreSQL） | ALTER TABLE 增加版本字段 | ✅ 已实现 |
| `SemanticTemplateController.java` | 版本查询通过现有部署历史 API 返回 | ✅ 已实现（无需新端点） |
| `services/semanticTemplate.ts` | 新增类型字段 | ✅ 已实现 |
| `DeployHistory.tsx` | 版本列 + 详情区版本/快照 + 对比按钮 + 对比弹窗 | ✅ 已实现 |
| 新建 `ConfigDiff.tsx` | 结构化变更摘要组件 + 原始 JSON diff 折叠区 | ✅ 已实现 |
| 新建 `diffUtils.ts` | `diffTemplateConfig()` 对比算法（纯函数） | ✅ 已实现 |
| `package.json` | 新增 `react-diff-viewer-continued` 依赖 | ✅ 已实现 |

### 前端：配置变更对比

采用两层对比设计：

**第一层：结构化变更摘要**（`diffUtils.ts` — 纯函数，可独立测试）

```typescript
interface ConfigChange {
  category: 'domain' | 'models' | 'dimensions' | 'measures'
           | 'dataSet' | 'agent' | 'terms' | 'configParams';
  type: 'added' | 'removed' | 'modified';
  name: string;
  detail?: string;
  parentName?: string;
}

function diffTemplateConfig(
  oldConfig: SemanticTemplateConfig,
  newConfig: SemanticTemplateConfig
): ConfigChange[]
```

对比维度与匹配规则：

| 对比对象 | 匹配 key | 对比字段 |
|---------|---------|---------|
| `domain` | 单例 | `name`, `bizName`, `description`, `viewers`, `admins` |
| `models[]` | `bizName` | `name`, `tableName`, `sqlQuery` |
| `models[].dimensions[]` | `bizName` | `name`, `fieldName`, `type`, `expr`, `alias` |
| `models[].measures[]` | `bizName` | `name`, `fieldName`, `aggOperator`, `expr` |
| `dataSet` | 单例 | `name`, `description`, `admins`, `viewers` |
| `agent` | 单例 | `name`, `description`, `examples[]` |
| `terms[]` | `name` | `description`, `alias[]` |
| `configParams[]` | `key` | `name`, `type`, `defaultValue`, `required` |

**第二层：原始 JSON Diff**（折叠区域，按需展开）

引入 `react-diff-viewer-continued`（`react-diff-viewer` 的活跃维护 fork，周下载 30w+，~40KB gzip），提供行级语法高亮的 side-by-side diff。

**对比交互流程**：

1. 用户在部署历史表格点击某行的"对比"按钮
2. 弹出对比弹窗，默认对比基准 = 同模板的上一个成功部署版本
3. 用户可通过下拉切换对比基准版本
4. 弹窗上方为结构化变更摘要（Collapse），下方为折叠的原始 JSON diff（按需展开）

部署历史表格列结构：

| 列 | 来源 | 说明 |
|----|------|------|
| 版本号 | `templateVersion` | 渲染为 `V{n}` |
| 部署时间 | `startTime` | 现有列 |
| 部署人 | `createdBy` | 现有列 |
| 部署状态 | `status` | 现有列 |
| 操作 | — | 详情 / **对比**（V1 无更早版本时不显示） |

## 待办

- 参数模板高级约束能力补强：枚举值动态来源统一（如 `optionsFrom=dimension:*` 的跨数据源一致性）
- 复杂范围约束（如日期跨度、数值上下限组合规则）
- 参数变更兼容策略（模板版本升级后老调度任务参数兼容）
- `MetricChangedEvent` + `ScheduleImpactListener`（建议独立迭代）
