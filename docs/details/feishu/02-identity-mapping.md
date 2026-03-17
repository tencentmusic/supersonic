---
status: implemented
module: feishu/server
key-files:
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuUserMappingService.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuContactService.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/rest/FeishuUserMappingController.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/persistence/dataobject/FeishuUserMappingDO.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/persistence/mapper/FeishuUserMappingMapper.java
depends-on: []
---

# 02 身份映射

## 目标

将飞书用户（`open_id`）映射到 SuperSonic 用户（`user_id`），使飞书用户能以正确身份调用查询链路，复用已有的行级权限、字段脱敏和 DataSet 访问控制。

## 当前状态

- [x] 共享字段自动匹配（工号 / 邮箱 / 手机号，按优先级）已实现
- [x] 自动匹配失败 → 创建 PENDING 记录 + 提示联系管理员已实现
- [x] 管理员手动编辑/启用映射（前端 + REST API）已实现
- [x] `FeishuContactService`（调用飞书通讯录 API 获取 employee_id/email/mobile）已实现
- [x] `SuperSonicApiClient` HTTP 调用（支持独立部署，不直连 DB）已实现
- [x] `s2_user.employee_id` 字段（V20 迁移）已实现
- [x] `s2_user_id` 允许 NULL（PENDING 状态，V21 迁移）已实现
- [x] `defaultAgentId` 字段（V22 迁移，支持多 Agent 路由）已实现
- [ ] 飞书 OAuth 自助绑定（仅有设计，不作为默认路径）
- [ ] 企业通讯录同步（阶段 2 可选，待评估）
- [ ] PENDING 记录自动过期（P3，待开发）

## 设计决策

**为什么不直接通过 DB 连接查 s2_user？**
身份映射服务通过 `SuperSonicApiClient` 调用 `/api/auth/user/*` REST API，而不是直连 DB。原因：支持 feishu-server 独立部署时与主服务解耦，同时保持认证、租户和审计链路一致。

**自动匹配策略优先级**：
1. `employee_id`（工号）— 企业内唯一，最可靠
2. `email`（企业邮箱）— 格式统一，常见
3. `phone`（手机号）— 常见，需注意隐私

**为什么 PENDING 状态的 `s2_user_id` 允许 NULL？**
自动匹配失败时，系统仍记录飞书用户的联系信息（姓名/邮箱/手机/工号），方便管理员在后台直接关联 s2 用户并启用。V21 迁移将该列改为可 NULL。

**OAuth 自助绑定为什么是可选而非默认？**
H5 页面输入平台账号密码的方式安全与信任成本较高，容易将飞书入口变为"反向登录入口"，与平台统一身份治理冲突。推荐顺序：自动匹配 → 管理员审核 → 企业通讯录同步。H5 绑定保留为设计但不建议作为默认路径。

**多 Agent 路由**：每个映射记录保存 `defaultAgentId`，管理员可在映射管理页面为每个用户设置默认 Agent。`FeishuBotService` 读取该字段写入 `FeishuMessage.agentId`，Handler 链全程使用 `msg.getAgentId()`，而非全局 `properties.getDefaultAgentId()`。

## 接口契约

### FeishuUserMappingService 核心方法

```java
// 解析飞书 open_id → SuperSonic User（含 PENDING 自动创建）
User resolveUser(String openId);

// 更新用户默认 Agent（/use 命令使用）
void updateDefaultAgent(String openId, int agentId);
```

### REST API（FeishuUserMappingController）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/feishu/userMappings` | 分页列表（current / pageSize） |
| `GET` | `/api/v1/feishu/userMappings/{id}` | 获取单条映射 |
| `POST` | `/api/v1/feishu/userMappings` | 创建映射 |
| `PATCH` | `/api/v1/feishu/userMappings/{id}` | 部分更新映射 |
| `DELETE` | `/api/v1/feishu/userMappings/{id}` | 删除映射 |
| `POST` | `/api/v1/feishu/userMappings/{id}:enable` | 启用映射 |
| `POST` | `/api/v1/feishu/userMappings/{id}:disable` | 禁用映射 |

### FeishuContactService

```java
// 调用飞书通讯录 API 获取用户联系信息
// GET /open-apis/contact/v3/users/{user_id}?user_id_type=open_id
FeishuUserInfo getUserInfo(String openId);
```

## 数据模型

### s2_feishu_user_mapping 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `feishu_open_id` | VARCHAR(64) UNIQUE | 飞书用户 Open ID |
| `feishu_union_id` | VARCHAR(64) | 飞书用户 Union ID（跨应用） |
| `feishu_user_name` | VARCHAR(128) | 飞书用户名（冗余，方便排查） |
| `feishu_email` | VARCHAR(128) | 飞书侧邮箱（匹配参考） |
| `feishu_mobile` | VARCHAR(20) | 飞书侧手机号（匹配参考） |
| `feishu_employee_id` | VARCHAR(64) | 飞书侧工号（匹配参考） |
| `s2_user_id` | BIGINT NULL | SuperSonic 用户 ID（PENDING 状态可为 NULL） |
| `s2_tenant_id` | BIGINT | 租户 ID |
| `default_agent_id` | INT | 该用户默认使用的 Agent |
| `match_type` | VARCHAR(20) | 映射来源：`AUTO_EMPLOYEE_ID` / `AUTO_EMAIL` / `AUTO_MOBILE` / `OAUTH_BIND` / `MANUAL` / `SYNC` / `PENDING` |
| `status` | TINYINT | 0-禁用 1-启用 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

索引：`UNIQUE KEY uk_feishu_open_id`、`KEY idx_s2_user_id`、`KEY idx_feishu_employee_id`

### s2_user 变更

| 列 | 变更 | 迁移脚本 |
|----|------|---------|
| `employee_id` VARCHAR(64) | V20 新增 | MySQL: `ALTER TABLE s2_user ADD COLUMN employee_id VARCHAR(64) DEFAULT NULL` |
| `s2_user_id` 允许 NULL | V21 变更 | 支持 PENDING 状态映射 |

## 实现要点

### 完整映射流程

```
收到飞书消息 (open_id)
  │
  ▼
查询 s2_feishu_user_mapping
  │
  ├─ 映射存在 + status=1 ──► 返回 SuperSonic 用户身份 ──► 继续查询
  │
  ├─ 映射存在 + status=0 ──► 回复卡片："您的查询权限已被暂停"
  │
  └─ 映射不存在
      │
      ▼
    调用飞书通讯录 API 获取用户信息（employee_id / email / phone）
      │
      ▼
    按 match-fields 优先级依次匹配 s2_user 表
      │
      ├─ 匹配成功 ──► 创建映射（match_type=AUTO_*）──► 继续查询
      │
      └─ 全部失败
          │
          ▼
        创建 PENDING 记录（status=0, matchType=PENDING）
        自动填充飞书联系人信息（姓名/邮箱/手机/工号）
          │
          └─ 回复卡片："已自动提交映射申请，请联系管理员审核"
```

### OAuth 自助绑定（已有设计，不作为默认路径）

仅在自动匹配和管理员审核均不可用时，作为可选兜底方案：
- 发飞书卡片，按钮跳转 H5 绑定页
- H5 页面由 `FeishuBindController` 提供，通过 `bindToken`（短效 JWT）与 open_id 关联
- 用户输入 SuperSonic 账号密码 → 验证通过 → 写入映射记录
- 安全约束：`bindToken` 有效期 10 分钟，一次性使用

### 企业通讯录同步（阶段 2 可选）

订阅飞书通讯录变更事件：

| 飞书事件 | 事件标识 | 系统动作 |
|---------|---------|---------|
| 员工入职 | `contact.user.created_v3` | 自动创建 SuperSonic 用户 + 映射记录 |
| 员工离职 | `contact.user.deleted_v3` | 禁用映射 + 禁用 SuperSonic 用户 |
| 信息变更 | `contact.user.updated_v3` | 同步更新用户名、部门等信息 |
| 部门变更 | `contact.department.updated_v3` | 同步更新组织架构（影响数据权限范围） |

## 待办

- [ ] 联调端到端（飞书真实 open_id → 自动匹配 → 查询）
- [ ] 管理后台 PENDING 数量告警（> 10 条时通知管理员）
- [ ] PENDING 记录超过 30 天自动标记 EXPIRED（P3）
- [ ] 评估企业通讯录同步可行性（阶段 2 可选）
