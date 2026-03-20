# AGENT-PROTOCOL.md — Agent 交接契约（含 ADR 守护）

## 交接要素总览

| 交接要素 | 要求 |
|---------|------|
| 输出物 | spec + 代码 + Scratchpad 条目 + 测试文件 + Prompt 版本号 |
| 自检报告 | VIBE-CHECKLIST 逐项 + Self-Critique 逐条 Anti-Goal 自查 + 放弃方案记录 + @AiGenerated 标注确认 |
| 下游验收 | 读 spec 后开工 + 上下文恢复确认 + ADR 合规性确认 |
| 阻断条件 | 3 项 spec 质量 + Scratchpad 缺失 + 测试缺失 + 推理链缺失 + Self-Critique 报告缺失 |
| Prompt 溯源 | 必须记录使用的 Prompt 版本 |

---

## 阶段一：需求 → SPEC Agent

### 输入
- 业务需求描述

### 输出
- intent.md（含 Anti-Goals）
- SCRATCHPAD 初始条目（含 SPEC 探索推理链）

### 验收
- [ ] intent.md 包含核心业务目标
- [ ] Anti-Goals 至少 3 条
- [ ] SCRATCHPAD 有推理链记录

---

## 阶段二：SPEC Agent → Architect Agent

### 交接包内容
- intent.md
- SCRATCHPAD 条目

### 输出
- openapi.yaml（API 契约）
- state-machines.yaml（状态流转）
- ADR（架构决策记录）

### 验收
- [ ] 《上下文恢复确认》已输出
- [ ] API 设计遵循 Google RESTful 风格
- [ ] 状态机覆盖所有业务流转

---

## 阶段三：Coding Agent → Review Agent（最关键）

### Self-Critique 自查（必须在交接前完成）

Coding Agent 在生成代码后、组织交接包之前，必须逐条检查以下内容：

- [ ] 对照 intent.md 的每条 Anti-Goal，逐条确认代码中没有违反
- [ ] 检查是否发明了 spec/openapi.yaml 中未定义的字段或接口
- [ ] 确认 DO 类有 tenantId 字段（非排除表）
- [ ] 确认 Controller 中不含 if/else 业务判断
- [ ] 确认没有使用 @Autowired 字段注入
- [ ] 确认没有使用 Class.forName 或 reflection 跨模块调用

**如有任何自查项未通过，Coding Agent 必须先修正代码，再组织交接包。未完成 Self-Critique 即提交交接包，等同于未跑测试直接提交。**

Self-Critique 检查结果必须作为《Self-Critique 自查报告》纳入交接包。

### 交接包内容
- 代码文件
- 测试文件（TEST-SPEC.md 规定的测试）
- 《生成决策说明》含推理链
- 《Self-Critique 自查报告》（逐条 Anti-Goal 检查结果）
- VIBE-CHECKLIST 检查记录（含第六关）
- Scratchpad 条目（含推理链）
- 使用的 Prompt 版本号（如 generate-api/v2.0）

### Review Agent 检查项

#### ADR 合规性检查
- [ ] 代码引入的依赖是否违反了 ADR 中的技术选型约束？
- [ ] 代码结构是否符合 ADR 中定义的分层架构决策？
- [ ] 有没有 AI 绕过了 ADR 中某个重要决策的情况？

#### SuperSonic 架构合规性检查
- [ ] 跨模块通信是否使用 ApplicationEvent（非 reflection）？
- [ ] 新 SPI 扩展是否在 spring.factories 注册？
- [ ] 多租户隔离是否正确（tenant_id 字段 + TenantSqlInterceptor）？

#### 测试质量检查
- [ ] Anti-Goals 中每条规则是否有对应测试方法？
- [ ] 测试方法名是否包含业务语义（@DisplayName）？
- [ ] 没有空测试或 @Disabled？

#### Prompt 可追溯性检查
- [ ] 交接包中是否包含 Prompt 版本号？
- [ ] 该 Prompt 版本是否是 PROMPT-VERSIONS/ 中当前推荐版本？
- [ ] 如果 Prompt 版本过旧，建议升级并重新生成

#### @AiGenerated 标注检查
- [ ] 复杂逻辑是否已标注 @AiGenerated？
- [ ] @AiGenerated 的 reviewBy 日期是否合理？
- [ ] 已到期的 @AiGenerated 是否已人工复核并移除？

### 阻断条件
- 测试文件缺失，阻断合并
- Anti-Goals 对应测试用例不完整，阻断合并
- Self-Critique 报告缺失，阻断合并
- Prompt 版本号缺失，要求补充（不阻断，但需记录）
- 存在过期 @AiGenerated（reviewBy 已过），标记为「一般」级问题
